package com.filmatube.app.data.social

import android.os.Bundle
import com.filmatube.app.di.IoDispatcher
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Social follow graph backed by `follows/{followerId}_{followedId}`.
 * Counts are derived by querying `follows` (rules block cross-user count writes).
 */
@Singleton
class FollowRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val analytics: FirebaseAnalytics,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val follows get() = firestore.collection("follows")
    private val myUid: String? get() = auth.currentUser?.uid
    private fun followId(followerId: String, followedId: String) = "${followerId}_$followedId"

    suspend fun setFollowing(targetUid: String, follow: Boolean) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        if (uid == targetUid) return@withContext
        val doc = follows.document(followId(uid, targetUid))
        if (follow) {
            doc.set(
                mapOf(
                    "followerId" to uid,
                    "followedId" to targetUid,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            analytics.logEvent("social_follow", Bundle().apply { putString("target_id", targetUid) })
        } else {
            doc.delete().await()
        }
    }

    fun isFollowing(targetUid: String): Flow<Boolean> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val registration = follows.document(followId(uid, targetUid))
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { registration.remove() }
    }

    fun observeFollowerIds(uid: String): Flow<List<String>> = callbackFlow {
        val registration = follows.whereEqualTo("followedId", uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.getString("followerId") } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    fun observeFollowingIds(uid: String): Flow<List<String>> = callbackFlow {
        val registration = follows.whereEqualTo("followerId", uid)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.mapNotNull { it.getString("followedId") } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** Ids the current user follows (for showing follow state in lists). */
    fun observeMyFollowingIds(): Flow<List<String>> =
        myUid?.let { observeFollowingIds(it) } ?: flowOf(emptyList())
}
