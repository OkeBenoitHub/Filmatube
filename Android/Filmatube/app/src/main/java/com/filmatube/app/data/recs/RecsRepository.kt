package com.filmatube.app.data.recs

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** One "Because you watched X" rail: the seed the row is explained by, and its movie ids. */
data class RecRow(
    val seedMovieId: String,
    val seedTitle: String,
    val seedPoster: String,
    val movieIds: List<String>,
)

/** A user's recommendations, as written by the nightly `buildRecommendations` function. */
data class Recommendations(
    val topPicks: List<String> = emptyList(),
    val rows: List<RecRow> = emptyList(),
) {
    val isEmpty: Boolean get() = topPicks.isEmpty() && rows.isEmpty()
}

/**
 * Reads the per-user recommendation doc and records "not interested" feedback.
 *
 * The doc is built server-side (Day 183) and is read-only here — the client never scores or
 * writes recs, it only reflects what the function produced and lets the user dismiss a title,
 * which the next nightly build reads back and excludes.
 */
@Singleton
class RecsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val myUid: String? get() = auth.currentUser?.uid

    /** This user's recommendations, or empty when the function hasn't built any yet. */
    suspend fun getRecommendations(): Recommendations = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext Recommendations()
        val doc = runCatching { firestore.collection("recs").document(uid).get().await() }
            .getOrNull() ?: return@withContext Recommendations()
        if (!doc.exists()) return@withContext Recommendations()

        @Suppress("UNCHECKED_CAST")
        val rawRows = (doc.get("rows") as? List<Map<String, Any?>>).orEmpty()
        Recommendations(
            topPicks = (doc.get("topPicks") as? List<*>)?.filterIsInstance<String>().orEmpty(),
            rows = rawRows.mapNotNull { row ->
                val ids = (row["movieIds"] as? List<*>)?.filterIsInstance<String>().orEmpty()
                if (ids.isEmpty()) return@mapNotNull null
                RecRow(
                    seedMovieId = row["seedMovieId"] as? String ?: "",
                    seedTitle = row["seedTitle"] as? String ?: "",
                    seedPoster = row["seedPoster"] as? String ?: "",
                    movieIds = ids,
                )
            },
        )
    }

    /**
     * "Not interested": the title stops being recommended from the next build on.
     *
     * Self-written to `recFeedback/{uid}/items/{movieId}`; the function reads these and
     * excludes them, so a dismissal sticks rather than reappearing on the next rebuild.
     */
    suspend fun dismiss(movieId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        runCatching {
            firestore.collection("recFeedback").document(uid).collection("items").document(movieId)
                .set(mapOf("action" to "dismissed", "createdAt" to FieldValue.serverTimestamp()))
                .await()
        }
    }
}
