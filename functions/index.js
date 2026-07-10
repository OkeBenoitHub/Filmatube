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
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");

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
