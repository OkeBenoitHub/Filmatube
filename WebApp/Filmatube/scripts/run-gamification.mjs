// Run the nightly gamification jobs on demand — the SAME code the Cloud Functions run
// (functions/gamification-core.js), so this is a rehearsal of the deployed job, not a copy.
//
// Usage (from WebApp/Filmatube):
//   node --env-file=.env.local scripts/run-gamification.mjs           # stats + badges
//   node --env-file=.env.local scripts/run-gamification.mjs --no-push # skip FCM, still writes inbox
//
// Safe to re-run: badge writes are idempotent (fixed doc ids) and the streak is counted once per
// calendar day, so a second run the same day won't inflate it.

import { cert, initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import core from "../../../functions/gamification-core.js";

const { awardAllBadges, buildAllStats } = core;
const noPush = process.argv.includes("--no-push");

initializeApp({
  credential: cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n"),
  }),
});
const db = getFirestore();

/** Mirrors the function's notifyUser: inbox doc + FCM, honouring the channel opt-in. */
async function notify(uid, { type, category, title, body, route, extra }) {
  const settings = await db.collection("users").doc(uid).collection("settings").doc("notifications").get();
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
  console.log(`   🔔 ${uid}: ${title}`);

  if (noPush) return true;
  const tokens = await db.collection("users").doc(uid).collection("fcmTokens").get();
  const ids = tokens.docs.map((t) => t.id);
  if (ids.length > 0) {
    try {
      await getMessaging().sendEachForMulticast({
        tokens: ids,
        notification: { title, body },
        data: { category, route },
      });
    } catch {
      /* non-fatal: the inbox doc is already written */
    }
  }
  return true;
}

console.log("▶ buildStats …");
const stats = await buildAllStats(db, FieldValue);
console.log(`  built ${stats.built}/${stats.total} users (catalogue ${stats.catalogue})`);

console.log("\n▶ awardBadges …");
const badges = await awardAllBadges(db, FieldValue, notify);
console.log(`  awarded ${badges.awarded} badge(s) across ${badges.total} users`);

// Show what each user ended up with, so the run is verifiable at a glance.
console.log("\n── Result ──");
const users = await db.collection("users").limit(20).get();
for (const u of users.docs) {
  const [s, b] = await Promise.all([
    db.collection("stats").doc(u.id).get(),
    db.collection("achievements").doc(u.id).collection("badges").get(),
  ]);
  if (!s.exists && b.empty) continue;
  const name = u.get("displayName") || u.id;
  const mins = s.get("totalWatchMinutes") ?? 0;
  console.log(
    `• ${name}: ${Math.floor(mins / 60)}h watched · ${s.get("moviesCompleted") ?? 0} movies · ` +
      `streak ${s.get("currentStreak") ?? 0} · week ${s.get("weeklyCompleted") ?? 0}/${s.get("weeklyGoal") ?? 3} · ` +
      `genres [${(s.get("topGenres") ?? []).join(", ")}] · badges [${b.docs.map((d) => d.id).join(", ")}]`,
  );
}

process.exit(0);
