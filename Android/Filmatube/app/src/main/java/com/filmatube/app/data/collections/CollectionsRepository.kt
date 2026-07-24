package com.filmatube.app.data.collections

import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.model.MovieCollection
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Read access to the user's collections (`collections/{id}` + `/items`). Created and edited on
 * the web; Android mirrors the Library page's collections section and opens a collection to view
 * its movies. The rules let a signed-in user read their own collection (userId == uid).
 */
@Singleton
class CollectionsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun map(id: String, data: Map<String, Any?>?): MovieCollection = MovieCollection(
        id = id,
        title = data?.get("title") as? String ?: "",
        coverUrl = data?.get("coverUrl") as? String ?: "",
        isPublic = data?.get("isPublic") as? Boolean ?: false,
    )

    /**
     * The signed-in user's collections, alphabetised (realtime). Filtered by `userId` only and
     * sorted in memory — index-safe, matching the web's `getUserCollections`.
     */
    fun observeMyCollections(): Flow<List<MovieCollection>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = firestore.collection("collections")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents
                    ?.map { map(it.id, it.data) }
                    ?.sortedBy { it.title.lowercase() }
                    ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** A single collection's metadata, or null if it's gone. */
    suspend fun getCollection(id: String): MovieCollection? = withContext(ioDispatcher) {
        val doc = runCatching { firestore.collection("collections").document(id).get().await() }
            .getOrNull() ?: return@withContext null
        if (!doc.exists()) return@withContext null
        map(doc.id, doc.data)
    }

    /** Ordered movie ids in a collection (by the `order` field, then insertion). */
    suspend fun getMovieIds(id: String): List<String> = withContext(ioDispatcher) {
        val snap = runCatching {
            firestore.collection("collections").document(id).collection("items").get().await()
        }.getOrNull() ?: return@withContext emptyList()
        snap.documents
            .map { it.id to ((it.getLong("order") ?: 0L)) }
            .sortedBy { it.second }
            .map { it.first }
    }
}
