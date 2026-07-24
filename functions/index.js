"use strict";

/**
 * Filmatube Cloud Functions (project: filmatubelive).
 *
 * Deploy as the Filmatube Firebase account:
 *   cd functions && npm install
 *   firebase deploy --only functions
 *
 * Requires the Blaze (pay-as-you-go) plan. Until deployed, the Android/Web clients
 * compute a live community average by reading `ratings/{movieId}/items` directly, so
 * the app works without this function — it just keeps `movies/{id}` denormalized for
 * catalog sorting/queries (e.g. "top rated").
 */

const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

/**
 * Roll up star ratings into the movie document whenever a user's rating changes.
 * Source:  ratings/{movieId}/items/{userId} = { value: 1..5, ... }
 * Target:  movies/{movieId}.averageRating (rounded to 1 dp) + ratingsCount
 */
// region must match the Firestore database location (eur3 since the 2026-07 migration) —
// gen-2 Firestore triggers refuse to deploy when the trigger location differs from the DB.
exports.aggregateRatings = onDocumentWritten({ document: "ratings/{movieId}/items/{userId}", region: "europe-west4" }, async (event) => {
  const movieId = event.params.movieId;
  const itemsSnap = await db.collection("ratings").doc(movieId).collection("items").get();

  let sum = 0;
  let count = 0;
  itemsSnap.forEach((doc) => {
    const value = doc.get("value");
    if (typeof value === "number" && value >= 1 && value <= 5) {
      sum += value;
      count += 1;
    }
  });

  const averageRating = count > 0 ? Math.round((sum / count) * 10) / 10 : 0;

  await db.collection("movies").doc(movieId).set({ averageRating, ratingsCount: count }, { merge: true });
});

/**
 * Keep `showtimes/{id}.attendeesCount` in step with the attendees subcollection.
 *
 * Showtime docs are admin-writable only (a public theater is a bigger spoofing target than
 * a board), so clients write just their own attendee doc and the count is maintained here.
 * Incremented by delta rather than recounted: a popular premiere can hold thousands of
 * attendees, and a full recount on every RSVP would be O(n) writes-per-write.
 */
exports.syncShowtimeAttendees = onDocumentWritten(
  { document: "showtimes/{showtimeId}/attendees/{userId}", region: "europe-west4" },
  async (event) => {
    const existedBefore = event.data?.before?.exists === true;
    const existsAfter = event.data?.after?.exists === true;
    const delta = (existsAfter ? 1 : 0) - (existedBefore ? 1 : 0);
    // Field-only edits (a remind toggle, a clock probe) don't move the count.
    if (delta === 0) return;

    await db
      .collection("showtimes")
      .doc(event.params.showtimeId)
      .set({ attendeesCount: FieldValue.increment(delta) }, { merge: true });
  },
);

/**
 * Keep `presentCount` on the showtime in step with the presence subcollection.
 *
 * This exists to kill an O(N^2) fanout. Clients used to subscribe to the whole presence
 * collection to count who was in the room: with N viewers each heartbeating every 30s, that
 * delivered N documents to N listeners — 1,000 viewers meant ~1M document reads per 30s.
 * One denormalized number on a document everyone already watches makes it O(N) writes and
 * zero extra listeners.
 *
 * Heartbeats are updates, not creates, so they don't move the count; only arrivals and
 * departures do. Stale entries (app killed without a clean exit) are swept by
 * `processTheaterSchedule`, whose deletes come back through here as decrements.
 */
exports.syncShowtimePresence = onDocumentWritten(
  { document: "showtimes/{showtimeId}/presence/{userId}", region: "europe-west4" },
  async (event) => {
    const delta = (event.data?.after?.exists ? 1 : 0) - (event.data?.before?.exists ? 1 : 0);
    if (delta === 0) return;
    await db
      .collection("showtimes")
      .doc(event.params.showtimeId)
      .set({ presentCount: FieldValue.increment(delta) }, { merge: true });
  },
);

/** Keep `waitlistCount` in step, mirroring how attendeesCount is maintained. */
exports.syncShowtimeWaitlist = onDocumentWritten(
  { document: "showtimes/{showtimeId}/waitlist/{userId}", region: "europe-west4" },
  async (event) => {
    const delta = (event.data?.after?.exists ? 1 : 0) - (event.data?.before?.exists ? 1 : 0);
    if (delta === 0) return;
    await db
      .collection("showtimes")
      .doc(event.params.showtimeId)
      .set({ waitlistCount: FieldValue.increment(delta) }, { merge: true });
  },
);

/**
 * When someone gives up a seat, pull the longest-waiting person in off the waitlist.
 *
 * Runs on attendee *removal* rather than on a schedule so the seat is filled in seconds —
 * a cancellation an hour before a premiere is worth nothing to the queue if it's noticed
 * after the film has started.
 */
exports.promoteFromWaitlist = onDocumentWritten(
  { document: "showtimes/{showtimeId}/attendees/{userId}", region: "europe-west4" },
  async (event) => {
    // Only a departure frees a seat.
    if (!event.data?.before?.exists || event.data?.after?.exists) return;

    const showtimeId = event.params.showtimeId;
    const ref = db.collection("showtimes").doc(showtimeId);
    const showtime = await ref.get();
    if (!showtime.exists || showtime.get("status") === "ended") return;

    const capacity = showtime.get("capacity") || 0;
    if (capacity <= 0) return; // unlimited rooms never have a queue

    // Read the count from the attendees collection rather than the denormalized field:
    // syncShowtimeAttendees may not have applied its decrement yet, and acting on a stale
    // count here would either overfill the room or refuse to promote anyone.
    const seated = await ref.collection("attendees").count().get();
    let free = capacity - seated.data().count;
    if (free <= 0) return;

    const queue = await ref.collection("waitlist").orderBy("joinedAt", "asc").limit(free).get();
    for (const entry of queue.docs) {
      try {
        // Claim the place by deleting it first: two concurrent departures would otherwise
        // both promote the same person and double-count the seat.
        await entry.ref.delete();
        await ref.collection("attendees").doc(entry.id).set({
          rsvp: true,
          remind: true,
          joinedAt: FieldValue.serverTimestamp(),
        });
        await notifyUser(entry.id, {
          type: "theater_seat_free",
          category: "content",
          title: showtime.get("movieTitle") || "",
          body: "A seat opened up — you're in.",
          route: `filmatube://showtime/${showtimeId}`,
          extra: { showtimeId, movieId: showtime.get("movieId") || "" },
        });
        free -= 1;
        console.log(`showtime ${showtimeId}: promoted ${entry.id} from the waitlist`);
      } catch (e) {
        console.error(`showtime ${showtimeId}: could not promote ${entry.id}`, e);
      }
      if (free <= 0) break;
    }
  },
);

/** Deliver one notification: inbox doc + FCM push, honoring the channel opt-in. */
async function notifyUser(uid, { type, category, title, body, route, extra }) {
  const settings = await db.collection("users").doc(uid).collection("settings").doc("notifications").get();
  // Absent settings means "not yet configured", which opts in — same as the broadcast path.
  if (settings.exists && settings.get(category) === false) return false;

  await db.collection("users").doc(uid).collection("notifications").add({
    type,
    actorName: "Filmatube",
    title,
    message: body,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
    ...extra,
  });

  const tokens = await db.collection("users").doc(uid).collection("fcmTokens").get();
  const ids = tokens.docs.map((t) => t.id);
  if (ids.length > 0) {
    try {
      await getMessaging().sendEachForMulticast({
        tokens: ids,
        notification: { title, body },
        data: { category, route },
      });
    } catch (e) {
      /* non-fatal: the inbox doc is already written */
    }
  }
  return true;
}

/** Doors open this long before the film rolls. */
const LOBBY_LEAD_MS = 15 * 60e3;
/** Fallback runtime when a showtime predates the denormalized `durationMs`. */
const FALLBACK_RUNTIME_MS = 3 * 3600e3;
/** A presence record older than this is treated as gone. Mirrors both clients. */
const PRESENCE_STALE_AFTER_MS = 90e3;

const RECURRENCE_STEP_MS = {
  daily: 24 * 3600e3,
  weekly: 7 * 24 * 3600e3,
};

/**
 * Walk showtimes through their lifecycle: scheduled → lobby → live → ended.
 *
 * Runs every minute. Late status flips are cosmetic rather than desynchronising: every client
 * derives its position from `startAt`, not from when the status changed, so a viewer who
 * joins a minute after the doors open still lands a minute into the film.
 *
 * Ending is `startAt + durationMs`, which stays correct across an admin pause for free —
 * resuming shifts `startAt` forward by the paused duration, carrying the end with it.
 */
exports.processTheaterSchedule = onSchedule(
  // Co-located with the eur3 database like the triggers are. A scheduled function doesn't
  // *require* it the way a Firestore trigger does, but this one runs every minute and does
  // several queries per run — from us-central1 each of those crossed the Atlantic.
  { schedule: "every 1 minutes", region: "europe-west4" },
  async () => {
  const now = Date.now();

  // Open the doors.
  const toLobby = await db
    .collection("showtimes")
    .where("status", "==", "scheduled")
    .where("startAt", "<=", new Date(now + LOBBY_LEAD_MS))
    .limit(20)
    .get();
  for (const doc of toLobby.docs) {
    await doc.ref.update({ status: "lobby" });
    console.log(`showtime ${doc.id}: scheduled → lobby`);
  }

  // Roll the film.
  const toLive = await db
    .collection("showtimes")
    .where("status", "==", "lobby")
    .where("startAt", "<=", new Date(now))
    .limit(20)
    .get();
  for (const doc of toLive.docs) {
    await doc.ref.update({ status: "live" });
    console.log(`showtime ${doc.id}: lobby → live`);
  }

  // Roll the credits. Can't be a range query — the end time is startAt + durationMs, which
  // Firestore can't compute server-side — so scan live showtimes and check each. There are
  // only ever a handful genuinely live at once, so the read cost is trivial.
  const live = await db.collection("showtimes").where("status", "==", "live").limit(50).get();
  for (const doc of live.docs) {
    // A paused room is frozen: its scheduled end hasn't arrived yet in effective time.
    if (doc.get("pausedAt")) continue;

    const startAt = doc.get("startAt");
    if (!startAt?.toMillis) continue;
    const runtime = doc.get("durationMs") || FALLBACK_RUNTIME_MS;
    if (now < startAt.toMillis() + runtime) continue;

    await doc.ref.update({ status: "ended", endedAt: FieldValue.serverTimestamp() });
    console.log(`showtime ${doc.id}: live → ended`);
    await spawnRecurrence(doc);
  }

  await sweepStalePresence(live.docs);
  },
);

/**
 * Remove presence records whose heartbeat has stopped.
 *
 * Without this, `presentCount` only ever grows: a viewer whose app is killed never writes the
 * delete that would decrement it, and the room would claim an audience long after everyone
 * left. The deletes flow back through `syncShowtimePresence` as decrements.
 */
async function sweepStalePresence(showtimeDocs) {
  const cutoff = Timestamp.fromMillis(Date.now() - PRESENCE_STALE_AFTER_MS);
  for (const showtime of showtimeDocs) {
    try {
      const stale = await showtime.ref
        .collection("presence")
        .where("presentAt", "<=", cutoff)
        .limit(200)
        .get();
      if (stale.empty) continue;

      const batch = db.batch();
      stale.docs.forEach((d) => batch.delete(d.ref));
      await batch.commit();
      console.log(`showtime ${showtime.id}: swept ${stale.size} stale presence records`);
    } catch (e) {
      console.error(`showtime ${showtime.id}: presence sweep failed`, e);
    }
  }
}

/**
 * Queue the next occurrence of a recurring showtime.
 *
 * Guarded by `recurrenceSpawned` on the finished doc rather than by looking for an existing
 * future showtime: an admin who deliberately deletes the next occurrence shouldn't have it
 * silently recreated on the following tick.
 */
async function spawnRecurrence(doc) {
  const step = RECURRENCE_STEP_MS[doc.get("recurrence")];
  if (!step || doc.get("recurrenceSpawned")) return;

  try {
    await doc.ref.update({ recurrenceSpawned: true });
    const startAt = doc.get("startAt");

    // Skip forward past any occurrences missed while the automation was down, so a paused
    // deployment doesn't schedule the next showing in the past.
    let next = startAt.toMillis() + step;
    while (next <= Date.now()) next += step;

    await db.collection("showtimes").add({
      movieId: doc.get("movieId") ?? "",
      movieTitle: doc.get("movieTitle") ?? "",
      posterUrl: doc.get("posterUrl") ?? "",
      backdropUrl: doc.get("backdropUrl") ?? "",
      startAt: Timestamp.fromMillis(next),
      status: "scheduled",
      // A repeat is by definition not a first screening, so the premiere badge doesn't carry.
      isPremiere: false,
      capacity: doc.get("capacity") ?? 0,
      attendeesCount: 0,
      durationMs: doc.get("durationMs") ?? 0,
      recurrence: doc.get("recurrence"),
      // Must be carried: the lineup query filters on boardId, so a repeat that omitted it
      // would be created successfully and then never appear on the schedule.
      boardId: doc.get("boardId") ?? "",
      waitlistCount: 0,
      createdAt: FieldValue.serverTimestamp(),
    });
    console.log(`showtime ${doc.id}: queued next ${doc.get("recurrence")} occurrence`);
  } catch (e) {
    console.error(`showtime ${doc.id}: could not queue next occurrence`, e);
  }
}

/**
 * When the credits roll: give the audience somewhere to talk, and ask them to rate it.
 *
 * Fires on the status transition rather than from the scheduler, so it also covers an admin
 * ending a showing by hand from the console.
 */
exports.onShowtimeEnded = onDocumentWritten(
  { document: "showtimes/{showtimeId}", region: "europe-west4" },
  async (event) => {
    const before = event.data?.before;
    const after = event.data?.after;
    if (!after?.exists) return;
    // Only the moment it becomes "ended" — not every later write to the doc.
    if (before?.get("status") === "ended" || after.get("status") !== "ended") return;

    const showtimeId = event.params.showtimeId;
    const movieId = after.get("movieId") || "";
    const movieTitle = after.get("movieTitle") || "";
    if (!movieId) return;

    const boardId = await ensureDiscussionBoard(movieId, movieTitle, after.get("posterUrl") || "");

    // Everyone who actually turned up — presence, not RSVPs. Someone who reserved a seat
    // and never came has nothing to discuss and nothing to rate.
    const present = await db.collection("showtimes").doc(showtimeId).collection("presence").limit(500).get();
    if (present.size === 500) {
      console.warn(`showtime ${showtimeId}: post-show fan-out capped at 500 attendees`);
    }

    for (const p of present.docs) {
      await notifyUser(p.id, {
        type: "theater_postshow",
        category: "social",
        title: movieTitle,
        body: "The showing has ended — rate it and join the discussion.",
        route: boardId ? `filmatube://board/${boardId}` : `filmatube://movie/${movieId}`,
        extra: { movieId, movieTitle, boardId, showtimeId },
      });
    }
    console.log(`showtime ${showtimeId}: post-show sent to ${present.size}, board ${boardId || "none"}`);
  },
);

/**
 * The movie's discussion board, creating it only if the film doesn't already have one.
 *
 * Reused rather than created per screening: a film shown weekly would otherwise accumulate a
 * graveyard of near-empty boards, and the conversation is about the movie, not the session.
 * Returns "" if it can't be resolved — the caller falls back to linking the movie itself.
 */
async function ensureDiscussionBoard(movieId, movieTitle, posterUrl) {
  try {
    const existing = await db
      .collection("boards")
      .where("type", "==", "movie")
      .where("movieId", "==", movieId)
      .limit(1)
      .get();
    if (!existing.empty) return existing.docs[0].id;

    const ref = db.collection("boards").doc();
    await ref.set({
      title: movieTitle,
      description: "",
      type: "movie",
      movieId,
      coverUrl: posterUrl,
      isPublic: true,
      isFeatured: false,
      // Marked official: this board is created by the theater itself, not by a member.
      isOfficial: true,
      // No human owner — a board the system opened has no one to hand moderation to, so it
      // stays admin-moderated. Rules treat a missing ownerId as "no member-owner".
      ownerId: "",
      memberIds: [],
      memberCount: 0,
      createdAt: FieldValue.serverTimestamp(),
    });
    return ref.id;
  } catch (e) {
    console.error(`could not resolve a discussion board for movie ${movieId}`, e);
    return "";
  }
}

const SOON_WINDOW_MS = 15 * 60e3;

/**
 * "Starting soon": nudge everyone who RSVP'd with reminders on, ~15 minutes out.
 *
 * `remindSentAt` on the showtime is the idempotency guard — this runs every 5 minutes and
 * the 15-minute window would otherwise match the same showtime three times running.
 */
exports.notifyShowtimeStartingSoon = onSchedule(
  { schedule: "every 5 minutes", region: "europe-west4" },
  async () => {
  const now = Date.now();
  const due = await db
    .collection("showtimes")
    .where("status", "in", ["scheduled", "lobby"])
    .where("startAt", "<=", new Date(now + SOON_WINDOW_MS))
    .limit(20)
    .get();

  for (const doc of due.docs) {
    if (doc.get("remindSentAt")) continue;
    // Claim it before fanning out, so a slow run can't be double-sent by the next tick.
    await doc.ref.update({ remindSentAt: FieldValue.serverTimestamp() });

    const title = doc.get("movieTitle") || "";
    const attendees = await doc.ref.collection("attendees").where("remind", "==", true).limit(500).get();
    let sent = 0;
    for (const a of attendees.docs) {
      const ok = await notifyUser(a.id, {
        type: "theater_starting",
        category: "content",
        title,
        body: "Starting soon in the theater.",
        route: `filmatube://showtime/${doc.id}`,
        extra: { showtimeId: doc.id, movieId: doc.get("movieId") || "" },
      });
      if (ok) sent += 1;
    }
    if (attendees.size === 500) {
      console.warn(`showtime ${doc.id}: reminder fan-out hit the 500-attendee cap; some RSVPs were not notified`);
    }
    console.log(`showtime ${doc.id}: reminded ${sent}/${attendees.size}`);
  }
});

const FRIEND_FANOUT_CAP = 200;

/**
 * "A friend is in a theater": when someone walks into a live showing, tell their followers.
 *
 * Guarded by a marker doc per (showtime, viewer) so leaving and rejoining — or a presence
 * heartbeat that recreates the doc after a network blip — can't re-notify the same
 * followers. Fan-out is capped; a very-followed account notifies a slice, not everyone.
 */
exports.notifyFriendInTheater = onDocumentWritten(
  { document: "showtimes/{showtimeId}/presence/{userId}", region: "europe-west4" },
  async (event) => {
    // Only on arrival, not on heartbeat updates or departures.
    if (event.data?.before?.exists || !event.data?.after?.exists) return;

    const { showtimeId, userId } = event.params;
    const showtime = await db.collection("showtimes").doc(showtimeId).get();
    if (!showtime.exists || showtime.get("status") !== "live") return;

    // Claim the (showtime, viewer) pair; a second arrival finds the marker and stops.
    const marker = db.collection("showtimes").doc(showtimeId).collection("friendNotified").doc(userId);
    try {
      await marker.create({ at: FieldValue.serverTimestamp() });
    } catch (e) {
      return; // already notified for this showing
    }

    const viewer = await db.collection("users").doc(userId).get();
    const viewerName = viewer.get("displayName") || "";
    const movieTitle = showtime.get("movieTitle") || "";

    const followers = await db.collection("follows").where("followedId", "==", userId).limit(FRIEND_FANOUT_CAP).get();
    if (followers.size === FRIEND_FANOUT_CAP) {
      console.warn(`user ${userId}: friend-in-theater fan-out capped at ${FRIEND_FANOUT_CAP} followers`);
    }
    for (const f of followers.docs) {
      await notifyUser(f.get("followerId"), {
        type: "friend_in_theater",
        category: "social",
        title: viewerName,
        body: `is watching ${movieTitle} in the theater.`,
        route: `filmatube://showtime/${showtimeId}`,
        extra: {
          showtimeId,
          actorId: userId,
          actorName: viewerName,
          actorAvatar: viewer.get("avatarUrl") || "",
          movieId: showtime.get("movieId") || "",
          movieTitle,
        },
      });
    }
  },
);

const ACTIVE_DAYS = 14;

/** Resolve target user ids for a broadcast segment. */
async function resolveRecipients(segment, genre) {
  let query = db.collection("users").limit(1000);
  if (segment === "taste" && genre) {
    query = db.collection("users").where("genrePreferences", "array-contains", genre).limit(1000);
  } else if (segment === "active") {
    const cutoff = new Date(Date.now() - ACTIVE_DAYS * 24 * 3600e3);
    query = db.collection("users").where("lastActiveAt", ">=", cutoff).limit(1000);
  }
  const snap = await query.get();
  return snap.docs.filter((d) => d.get("isBanned") !== true).map((d) => d.id);
}

/** Fan a broadcast out to inboxes (honoring the system opt-in) + FCM push. */
async function deliverBroadcast(uids, title, body, movieId) {
  const tokens = [];
  let delivered = 0;
  for (let i = 0; i < uids.length; i += 400) {
    const group = uids.slice(i, i + 400);
    const batch = db.batch();
    for (const uid of group) {
      const settings = await db.collection("users").doc(uid).collection("settings").doc("notifications").get();
      if (settings.exists && settings.get("system") === false) continue;
      const ref = db.collection("users").doc(uid).collection("notifications").doc();
      batch.set(ref, {
        type: "system",
        actorName: "Filmatube",
        title,
        message: body,
        movieId: movieId || "",
        read: false,
        createdAt: FieldValue.serverTimestamp(),
      });
      delivered += 1;
      const ts = await db.collection("users").doc(uid).collection("fcmTokens").get();
      ts.forEach((t) => tokens.push(t.id));
    }
    await batch.commit();
  }
  for (let i = 0; i < tokens.length; i += 500) {
    try {
      await getMessaging().sendEachForMulticast({
        tokens: tokens.slice(i, i + 500),
        notification: { title, body },
        data: { category: "system", route: movieId ? `/movie/${movieId}` : "/notifications" },
      });
    } catch (e) {
      /* non-fatal */
    }
  }
  return delivered;
}

/**
 * Process scheduled broadcasts whose due time has arrived. Immediate sends are handled
 * server-side by the web admin action; this only picks up `status: "scheduled"` docs.
 */
exports.processScheduledBroadcasts = onSchedule(
  { schedule: "every 5 minutes", region: "europe-west4" },
  async () => {
  const now = new Date();
  const due = await db
    .collection("broadcasts")
    .where("status", "==", "scheduled")
    .where("scheduledAt", "<=", now)
    .limit(10)
    .get();

  for (const doc of due.docs) {
    const b = doc.data();
    const uids = await resolveRecipients(b.segment, b.genre);
    const delivered = await deliverBroadcast(uids, b.title, b.body, b.movieId);
    await doc.ref.update({ status: "sent", sentAt: FieldValue.serverTimestamp(), recipientCount: delivered });
  }
});

// ───────── referral rewards (v1.3, Day 199) ─────────

/** The perk a successful referrer unlocks: badge id + entitlement. */
const RECRUITER_BADGE = "recruiter";

/**
 * When a referral is recorded (`referrals/{referredId}` created — server-write-only), reward and
 * notify the referrer: bump their referral count, grant the Recruiter badge and early-premiere
 * access, and push a notification. Trigger-based so it fires for both attribution paths (web
 * session route and the Android `/api/referral` endpoint).
 */
exports.onReferralCreated = onDocumentWritten(
  { document: "referrals/{referredId}", region: "europe-west4" },
  async (event) => {
    const before = event.data?.before;
    const after = event.data?.after;
    if (!after?.exists || before?.exists) return; // creates only

    const referrerId = after.get("referrerId");
    const referredId = after.get("referredId") || event.params.referredId;
    if (!referrerId || referrerId === referredId) return;

    // Reward the referrer. Cross-user write, which only the admin SDK (this function) may do.
    await db.collection("users").doc(referrerId).set(
      {
        referralCount: FieldValue.increment(1),
        badges: FieldValue.arrayUnion(RECRUITER_BADGE),
        earlyAccess: true,
      },
      { merge: true },
    );

    const referred = await db.collection("users").doc(referredId).get();
    const name = (referred.exists && referred.get("displayName")) || "A friend";

    await notifyUser(referrerId, {
      type: "referral",
      category: "social",
      title: "You earned Recruiter 🎬",
      body: `${name} joined Filmatube through your invite. Early premiere access unlocked.`,
      route: "/refer",
      extra: { movieId: "" },
    });
  },
);

// ───────── recommendations (v1.3, Day 183) ─────────
//
// Scoring lives in ./recs-core.js so this scheduled pass and scripts/seed-recs-demo.mjs run
// the identical ranking. See that module for the algorithm.

const { buildAllRecs } = require("./recs-core");

/**
 * Nightly content-overlap recommender — no ML. Reads the published catalogue once, then for
 * each active user accumulates a taste profile from the signals they already leave and scores
 * every unseen movie by genre/person overlap, writing recs/{uid}. A scheduled batch rather
 * than a per-write trigger: recs change slowly, so one nightly pass beats recomputing on every
 * like.
 */
exports.buildRecommendations = onSchedule(
  { schedule: "every 24 hours", region: "europe-west4" },
  async () => {
    const { built, total, catalogue } = await buildAllRecs(db, FieldValue);
    console.log(`recs: built ${built}/${total} from a catalogue of ${catalogue}`);
  },
);
