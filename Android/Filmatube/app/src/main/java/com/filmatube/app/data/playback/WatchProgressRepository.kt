package com.filmatube.app.data.playback

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists per-user watch progress to `watchProgress/{uid}/items/{movieId}`
 * (owner-scoped by the security rules). Feeds the Continue Watching row and is
 * marked `completed` once the viewer passes [COMPLETE_THRESHOLD].
 */
@Singleton
class WatchProgressRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun itemDoc(uid: String, movieId: String) =
        firestore.collection("watchProgress").document(uid).collection("items").document(movieId)

    /** Upsert the current position; no-op if signed out or duration is unknown. */
    suspend fun save(movieId: String, positionMs: Long, durationMs: Long) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        if (durationMs <= 0L) return@withContext
        val progress = (positionMs.toDouble() / durationMs).coerceIn(0.0, 1.0)
        val data = mapOf(
            "movieId" to movieId,
            "positionMs" to positionMs,
            "durationMs" to durationMs,
            "progress" to progress,
            "completed" to (progress >= COMPLETE_THRESHOLD),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        runCatching { itemDoc(uid, movieId).set(data, SetOptions.merge()).await() }
    }

    /** Saved resume position in ms, or 0 if none / already completed. */
    suspend fun resumePosition(movieId: String): Long = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext 0L
        runCatching {
            val snapshot = itemDoc(uid, movieId).get().await()
            if (snapshot.getBoolean("completed") == true) 0L else (snapshot.getLong("positionMs") ?: 0L)
        }.getOrDefault(0L)
    }

    companion object {
        private const val COMPLETE_THRESHOLD = 0.9 // watched ≥ 90% ⇒ completed
    }
}
