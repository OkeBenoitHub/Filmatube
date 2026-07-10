package com.filmatube.app.data.social

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Community rating aggregate for a movie, computed live from `ratings/{movieId}/items`. */
data class RatingAggregate(val average: Double, val count: Int)

/**
 * Star ratings (1–5) at `ratings/{movieId}/items/{uid}` — one doc per user per movie.
 * A Cloud Function (functions/index.js) rolls these up into `movies/{id}.averageRating`/
 * `ratingsCount`; clients also read the subtree directly for an instant community average.
 */
@Singleton
class RatingRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun items(movieId: String) =
        firestore.collection("ratings").document(movieId).collection("items")

    /** The current user's own rating for [movieId], or null if not yet rated. */
    fun observeMyRating(movieId: String): Flow<Int?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val registration = items(movieId).document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.getLong("value")?.toInt()) }
        awaitClose { registration.remove() }
    }

    /** Live community aggregate (average + count) across all users' ratings for [movieId]. */
    fun observeAggregate(movieId: String): Flow<RatingAggregate> = callbackFlow {
        val registration = items(movieId).addSnapshotListener { snap, _ ->
            val values = snap?.documents?.mapNotNull { it.getLong("value")?.toInt() } ?: emptyList()
            val avg = if (values.isEmpty()) 0.0 else values.sum().toDouble() / values.size
            trySend(RatingAggregate(average = avg, count = values.size))
        }
        awaitClose { registration.remove() }
    }

    /** Set (1–5) or clear (null) the current user's rating for [movieId]. */
    suspend fun setRating(movieId: String, value: Int?) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val doc = items(movieId).document(uid)
        if (value == null) {
            doc.delete().await()
        } else {
            doc.set(
                mapOf(
                    "movieId" to movieId,
                    "userId" to uid,
                    "value" to value.coerceIn(1, 5),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }
}
