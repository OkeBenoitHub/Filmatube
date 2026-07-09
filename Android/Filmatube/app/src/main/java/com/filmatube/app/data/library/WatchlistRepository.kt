package com.filmatube.app.data.library

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** "Watch Later" list backed by `watchlists/{uid}/movies` (owner-scoped by the rules). */
@Singleton
class WatchlistRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun movies(uid: String) =
        firestore.collection("watchlists").document(uid).collection("movies")

    /** Saved movie ids, most-recent first (realtime). */
    fun observeSavedIds(): Flow<List<String>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = movies(uid)
            .orderBy("addedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents?.map { it.id } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** Add or remove a movie from Watch Later. */
    suspend fun toggle(movieId: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val doc = movies(uid).document(movieId)
        val exists = doc.get().await().exists()
        if (exists) {
            doc.delete().await()
        } else {
            doc.set(mapOf("movieId" to movieId, "addedAt" to FieldValue.serverTimestamp())).await()
        }
    }
}
