"use strict";

/**
 * Shared achievements + stats core (v1.3, Days 200–202).
 *
 * Extracted from index.js — exactly like recs-core.js — so the nightly Cloud Functions AND the
 * ops script `scripts/run-gamification.mjs` run the *same* logic. A manual run is therefore a
 * genuine rehearsal of the deployed job, not a copy that can drift from it.
 *
 * Dependency-free: `db`, `FieldValue` and a `notify(uid, payload)` callback are passed in, so the
 * module works identically inside a Cloud Function and in a one-off admin script.
 */

/** Only sweep accounts active within this window. */
const ACTIVE_DAYS = 30;

/** Movies-a-week target used until a user sets their own. */
const WEEKLY_GOAL_DEFAULT = 3;

/** UTC calendar day, the unit a streak is counted in. */
const dayKey = (d) => d.toISOString().slice(0, 10);

/**
 * Every badge, with the signal threshold that unlocks it. Signals are gathered once per user in
 * [evaluateBadges]; the ids match FIRESTORE_SCHEMA's achievements/{uid}/badges/{badgeId}.
 */
const BADGES = [
  { id: "first_watch", name: "First Watch", test: (s) => s.watched >= 1 },
  { id: "binge_watcher", name: "Binge Watcher", test: (s) => s.watched >= 10 },
  { id: "cinephile", name: "Cinephile", test: (s) => s.watched >= 25 },
  { id: "critic", name: "Critic", test: (s) => s.reviews >= 5 },
  { id: "social_butterfly", name: "Social Butterfly", test: (s) => s.following >= 10 },
  { id: "premiere_goer", name: "Premiere Goer", test: (s) => s.premieres >= 1 },
  { id: "recruiter", name: "Recruiter", test: (s) => s.referrals >= 1 },
];

/** Recently-active users — the population both nightly jobs sweep. */
function activeUsers(db) {
  const cutoff = new Date(Date.now() - ACTIVE_DAYS * 24 * 3600e3);
  return db.collection("users").where("lastActiveAt", ">=", cutoff).limit(500).get();
}

/** Write one badge (idempotent — fixed doc id); does not re-notify on repeats. */
async function awardBadge(db, FieldValue, uid, badgeId) {
  await db.collection("achievements").doc(uid).collection("badges").doc(badgeId).set(
    { badgeId, unlockedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
}

/** Award any newly-earned badges for one user and notify once each. Returns the ids awarded. */
async function evaluateBadges(db, FieldValue, notify, uid, userData) {
  const [watched, following, stats, owned] = await Promise.all([
    db.collection("watchProgress").doc(uid).collection("items").where("completed", "==", true).count().get(),
    db.collection("follows").where("followerId", "==", uid).count().get(),
    db.collection("stats").doc(uid).get(),
    db.collection("achievements").doc(uid).collection("badges").get(),
  ]);
  const signals = {
    watched: watched.data().count,
    following: following.data().count,
    reviews: (stats.exists && stats.get("reviewsWritten")) || 0,
    premieres: (stats.exists && stats.get("premieresAttended")) || 0,
    referrals: userData.referralCount || 0,
  };
  const have = new Set(owned.docs.map((d) => d.id));
  const newly = BADGES.filter((b) => b.test(signals) && !have.has(b.id));
  for (const b of newly) {
    await awardBadge(db, FieldValue, uid, b.id);
    await notify(uid, {
      type: "badge",
      category: "social",
      title: `You earned ${b.name} 🏅`,
      body: `New achievement unlocked: ${b.name}.`,
      route: "/account",
      extra: { movieId: "" },
    });
  }
  return newly.map((b) => b.id);
}

/** Nightly badge sweep over active users. Returns a summary. */
async function awardAllBadges(db, FieldValue, notify) {
  const users = await activeUsers(db);
  let awarded = 0;
  const perUser = {};
  for (const u of users.docs) {
    try {
      const ids = await evaluateBadges(db, FieldValue, notify, u.id, u.data());
      if (ids.length > 0) perUser[u.id] = ids;
      awarded += ids.length;
    } catch (e) {
      console.error(`badges: failed for ${u.id}`, e);
    }
  }
  return { awarded, total: users.size, perUser };
}

/**
 * Streak fields for one user (Day 202), derived from `users.lastActiveAt`.
 *
 * Counted once per calendar day of activity — re-running the job the same day is a no-op, so the
 * streak can't be inflated by an extra pass. Activity on the day after the last counted day
 * extends the run; any longer gap restarts it at 1.
 */
function streakUpdate(lastActiveAt, prev) {
  const lastActive = lastActiveAt?.toDate?.();
  if (!lastActive) return {};

  const activeDay = dayKey(lastActive);
  const countedDay = prev.exists ? prev.get("streakLastDay") : null;
  if (countedDay === activeDay) return {}; // already counted this day

  const streak = (prev.exists && prev.get("currentStreak")) || 0;
  const longest = (prev.exists && prev.get("longestStreak")) || 0;
  const dayAfterCounted = countedDay ? dayKey(new Date(Date.parse(countedDay) + 24 * 3600e3)) : null;

  const currentStreak = dayAfterCounted === activeDay ? streak + 1 : 1;
  return {
    currentStreak,
    longestStreak: Math.max(longest, currentStreak),
    streakLastDay: activeDay,
  };
}

/**
 * Nightly stats roll-up → `stats/{uid}`: watch minutes, movies completed, top genres, streak and
 * weekly-goal progress. Merged, so the trigger-maintained `reviewsWritten` / `premieresAttended`
 * survive. Watch time counts partial views (duration × progress) — time actually spent, not only
 * finished films.
 */
async function buildAllStats(db, FieldValue) {
  const moviesSnap = await db.collection("movies").where("status", "==", "published").get();
  const byId = new Map(moviesSnap.docs.map((d) => [d.id, d.data()]));

  const users = await activeUsers(db);
  const weekAgo = Date.now() - 7 * 24 * 3600e3;
  let built = 0;

  for (const user of users.docs) {
    try {
      const [items, prev] = await Promise.all([
        db.collection("watchProgress").doc(user.id).collection("items").get(),
        db.collection("stats").doc(user.id).get(),
      ]);
      let minutes = 0;
      let completed = 0;
      let weeklyCompleted = 0;
      const genreTally = new Map();

      for (const item of items.docs) {
        const movie = byId.get(item.get("movieId") || item.id);
        if (!movie) continue;
        const done = item.get("completed") === true;
        const progress = Math.min(1, Math.max(0, Number(item.get("progress") ?? 0)));
        minutes += Number(movie.duration ?? 0) * (done ? 1 : progress);
        if (done) {
          completed += 1;
          (movie.genres ?? []).forEach((g) => genreTally.set(g, (genreTally.get(g) ?? 0) + 1));
          const touched = item.get("updatedAt")?.toMillis?.() ?? 0;
          if (touched >= weekAgo) weeklyCompleted += 1;
        }
      }

      const topGenres = [...genreTally.entries()]
        .sort((a, b) => b[1] - a[1])
        .slice(0, 3)
        .map(([g]) => g);

      await db.collection("stats").doc(user.id).set(
        {
          totalWatchMinutes: Math.round(minutes),
          moviesCompleted: completed,
          topGenres,
          weeklyCompleted,
          weeklyGoal: prev.get("weeklyGoal") || WEEKLY_GOAL_DEFAULT,
          ...streakUpdate(user.get("lastActiveAt"), prev),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      built += 1;
    } catch (e) {
      console.error(`stats: failed for ${user.id}`, e);
    }
  }
  return { built, total: users.size, catalogue: moviesSnap.size };
}

module.exports = {
  BADGES,
  WEEKLY_GOAL_DEFAULT,
  awardBadge,
  evaluateBadges,
  awardAllBadges,
  streakUpdate,
  buildAllStats,
};
