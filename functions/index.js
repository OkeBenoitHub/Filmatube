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
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();
const db = getFirestore();

/**
 * Roll up star ratings into the movie document whenever a user's rating changes.
 * Source:  ratings/{movieId}/items/{userId} = { value: 1..5, ... }
 * Target:  movies/{movieId}.averageRating (rounded to 1 dp) + ratingsCount
 */
exports.aggregateRatings = onDocumentWritten("ratings/{movieId}/items/{userId}", async (event) => {
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
exports.processScheduledBroadcasts = onSchedule("every 5 minutes", async () => {
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
