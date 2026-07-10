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

/** The four movie reactions. */
enum class ReactionType(val value: String, val emoji: String) {
    LOVE("love", "❤️"),
    FIRE("fire", "🔥"),
    MIND_BLOWN("mind_blown", "🤯"),
    BORING("boring", "😴"),
}

/** Per-user reactions at `reactions/{uid}/items/{movieId}`; counts via a collection-group query. */
@Singleton
class ReactionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun reactionDoc(uid: String, movieId: String) =
        firestore.collection("reactions").document(uid).collection("items").document(movieId)

    fun observeMyReaction(movieId: String): Flow<String?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val registration = reactionDoc(uid, movieId)
            .addSnapshotListener { snap, _ -> trySend(snap?.getString("type")) }
        awaitClose { registration.remove() }
    }

    suspend fun setReaction(movieId: String, type: String?) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val doc = reactionDoc(uid, movieId)
        if (type == null) {
            doc.delete().await()
        } else {
            doc.set(
                mapOf(
                    "movieId" to movieId,
                    "type" to type,
                    "kind" to "reaction",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    /** Reaction counts by type across all users (needs the items(movieId,kind) collection-group index). */
    suspend fun reactionCounts(movieId: String): Map<String, Int> = withContext(ioDispatcher) {
        runCatching {
            firestore.collectionGroup("items")
                .whereEqualTo("movieId", movieId)
                .whereEqualTo("kind", "reaction")
                .get().await()
                .documents
                .groupingBy { it.getString("type") ?: "" }
                .eachCount()
                .filterKeys { it.isNotBlank() }
        }.getOrDefault(emptyMap())
    }
}
