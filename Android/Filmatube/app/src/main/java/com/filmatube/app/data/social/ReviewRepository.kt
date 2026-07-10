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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** A public written review of a movie. */
data class Review(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val hasSpoiler: Boolean,
    val createdAtMs: Long,
    val likeCount: Int,
    val likedByMe: Boolean,
    val isMine: Boolean,
)

/**
 * Public reviews at `reviews/{movieId}/items/{uid}` (one per user, editable/overwritten).
 * Likes live at `reviews/{movieId}/items/{reviewId}/likes/{uid}`; counts are read from the
 * subtree (owners can't be granted write to other users' review docs by rules).
 */
@Singleton
class ReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun items(movieId: String) =
        firestore.collection("reviews").document(movieId).collection("items")

    /** All reviews for [movieId], newest first, each enriched with like count + my-like state. */
    fun observeReviews(movieId: String): Flow<List<Review>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val registration = items(movieId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents ?: emptyList()
                launch {
                    val reviews = docs.map { d ->
                        val likes = runCatching { d.reference.collection("likes").get().await() }.getOrNull()
                        Review(
                            id = d.id,
                            userId = d.getString("userId") ?: "",
                            userName = d.getString("userName") ?: "",
                            userAvatar = d.getString("userAvatar") ?: "",
                            text = d.getString("text") ?: "",
                            hasSpoiler = d.getBoolean("hasSpoiler") ?: false,
                            createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            likeCount = likes?.size() ?: 0,
                            likedByMe = uid != null && likes?.documents?.any { it.id == uid } == true,
                            isMine = uid != null && d.id == uid,
                        )
                    }
                    trySend(reviews)
                }
            }
        awaitClose { registration.remove() }
    }

    /** Create or overwrite the current user's review for [movieId]. */
    suspend fun submitReview(movieId: String, text: String, hasSpoiler: Boolean) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        items(movieId).document(uid).set(
            mapOf(
                "userId" to uid,
                "userName" to (me?.displayName ?: ""),
                "userAvatar" to (me?.avatarUrl ?: ""),
                "text" to text.trim(),
                "hasSpoiler" to hasSpoiler,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
    }

    /** Delete the current user's review. */
    suspend fun deleteReview(movieId: String) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        items(movieId).document(uid).delete().await()
    }

    /** Toggle the current user's like on a review. */
    suspend fun toggleLike(movieId: String, reviewId: String, currentlyLiked: Boolean) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val doc = items(movieId).document(reviewId).collection("likes").document(uid)
        if (currentlyLiked) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
        }
    }
}
