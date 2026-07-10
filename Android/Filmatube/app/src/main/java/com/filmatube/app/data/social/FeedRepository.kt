package com.filmatube.app.data.social

import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** An activity-feed entry (something a followed user did). */
data class FeedEvent(
    val id: String,
    val actorName: String,
    val actorAvatar: String,
    val type: String,
    val movieId: String,
    val movieTitle: String,
    val createdAtMs: Long,
)

object FeedEventTypes {
    const val WATCHING = "watching"
    const val WATCHED = "watched"
    const val ADDED_WATCHLIST = "added_watchlist"
    const val LIKED = "liked"
    const val REACTED = "reacted"
    const val ADDED_COLLECTION = "added_collection"
}

/**
 * Activity feed at `feed/{uid}/events`. Publishing **fans out** an event to each of the actor's
 * followers' feeds (the rules allow an actor to write their own event into a follower's inbox).
 * A server-side Cloud Function can replace this client fan-out later.
 */
@Singleton
class FeedRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun events(uid: String) = firestore.collection("feed").document(uid).collection("events")

    /** The current user's feed (their followees' activity), newest first. */
    fun observeFeed(): Flow<List<FeedEvent>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = events(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { d ->
                    FeedEvent(
                        id = d.id,
                        actorName = d.getString("actorName") ?: "",
                        actorAvatar = d.getString("actorAvatar") ?: "",
                        type = d.getString("type") ?: "",
                        movieId = d.getString("movieId") ?: "",
                        movieTitle = d.getString("movieTitle") ?: "",
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Fan-out an activity event to the current user's followers. Best-effort. */
    suspend fun publish(type: String, movieId: String, movieTitle: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val followers = runCatching { followRepository.observeFollowerIds(uid).first() }.getOrDefault(emptyList())
        if (followers.isEmpty()) return@withContext
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()

        val batch = firestore.batch()
        followers.forEach { followerUid ->
            batch.set(
                events(followerUid).document(),
                mapOf(
                    "actorId" to uid,
                    "actorName" to (me?.displayName ?: ""),
                    "actorAvatar" to (me?.avatarUrl ?: ""),
                    "type" to type,
                    "movieId" to movieId,
                    "movieTitle" to movieTitle,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            )
        }
        runCatching { batch.commit().await() }
    }
}
