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

/** A movie recommended to the current user by someone they follow / who follows them. */
data class Recommendation(
    val id: String,
    val fromName: String,
    val fromAvatar: String,
    val movieId: String,
    val movieTitle: String,
    val message: String,
    val createdAtMs: Long,
)

/** A possible recommendation recipient (someone the current user follows). */
data class RecipientUser(val uid: String, val displayName: String, val avatarUrl: String)

/** Movie recommendations at `recommendations/{toUid}/items/{id}`. */
@Singleton
class RecommendationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun inbox(uid: String) =
        firestore.collection("recommendations").document(uid).collection("items")

    /** Recommendations received by the current user, newest first. */
    fun observeInbox(): Flow<List<Recommendation>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = inbox(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                trySend(
                    snap?.documents?.map { d ->
                        Recommendation(
                            id = d.id,
                            fromName = d.getString("fromName") ?: "",
                            fromAvatar = d.getString("fromAvatar") ?: "",
                            movieId = d.getString("movieId") ?: "",
                            movieTitle = d.getString("movieTitle") ?: "",
                            message = d.getString("message") ?: "",
                            createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        )
                    } ?: emptyList(),
                )
            }
        awaitClose { registration.remove() }
    }

    /** Send a recommendation to [toUid] with an optional [message]. */
    suspend fun send(toUid: String, movieId: String, movieTitle: String, message: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        inbox(toUid).document().set(
            mapOf(
                "fromUserId" to uid,
                "fromName" to (me?.displayName ?: ""),
                "fromAvatar" to (me?.avatarUrl ?: ""),
                "movieId" to movieId,
                "movieTitle" to movieTitle,
                "message" to message,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** People the current user follows — recommendation recipients. */
    suspend fun recipients(): List<RecipientUser> = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext emptyList()
        val ids = runCatching { followRepository.observeFollowingIds(uid).first() }.getOrDefault(emptyList())
        ids.mapNotNull { id ->
            userRepository.getUser(id)?.let { RecipientUser(it.uid, it.displayName, it.avatarUrl) }
        }
    }
}
