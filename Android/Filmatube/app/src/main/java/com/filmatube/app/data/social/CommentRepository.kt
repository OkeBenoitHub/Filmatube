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

/** A comment (or reply, when [parentId] is set) on a movie. */
data class Comment(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val hasSpoiler: Boolean,
    val parentId: String?,
    val createdAtMs: Long,
    val likeCount: Int,
    val likedByMe: Boolean,
    val isMine: Boolean,
)

/**
 * Threaded comments at `comments/{movieId}/items/{commentId}` (one level of replies via
 * `parentId`). Likes at `.../likes/{uid}`; counts read from the subtree.
 */
@Singleton
class CommentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private fun items(movieId: String) =
        firestore.collection("comments").document(movieId).collection("items")

    /** All comments for [movieId], oldest first, enriched with like count + my-like state. */
    fun observeComments(movieId: String): Flow<List<Comment>> = callbackFlow {
        val uid = auth.currentUser?.uid
        val registration = items(movieId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
                val docs = snap?.documents ?: emptyList()
                launch {
                    val comments = docs.map { d ->
                        val likes = runCatching { d.reference.collection("likes").get().await() }.getOrNull()
                        Comment(
                            id = d.id,
                            userId = d.getString("userId") ?: "",
                            userName = d.getString("userName") ?: "",
                            userAvatar = d.getString("userAvatar") ?: "",
                            text = d.getString("text") ?: "",
                            hasSpoiler = d.getBoolean("hasSpoiler") ?: false,
                            parentId = d.getString("parentId"),
                            createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            likeCount = likes?.size() ?: 0,
                            likedByMe = uid != null && likes?.documents?.any { it.id == uid } == true,
                            isMine = uid != null && d.getString("userId") == uid,
                        )
                    }
                    trySend(comments)
                }
            }
        awaitClose { registration.remove() }
    }

    /** Post a top-level comment (parentId null) or a reply. */
    suspend fun addComment(movieId: String, text: String, hasSpoiler: Boolean, parentId: String?) =
        withContext(ioDispatcher) {
            val uid = auth.currentUser?.uid ?: return@withContext
            val me = runCatching { userRepository.getUser(uid) }.getOrNull()
            items(movieId).add(
                mapOf(
                    "userId" to uid,
                    "userName" to (me?.displayName ?: ""),
                    "userAvatar" to (me?.avatarUrl ?: ""),
                    "text" to text.trim(),
                    "hasSpoiler" to hasSpoiler,
                    "parentId" to parentId,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }

    suspend fun deleteComment(movieId: String, commentId: String) = withContext(ioDispatcher) {
        items(movieId).document(commentId).delete().await()
    }

    suspend fun toggleLike(movieId: String, commentId: String, currentlyLiked: Boolean) = withContext(ioDispatcher) {
        val uid = auth.currentUser?.uid ?: return@withContext
        val doc = items(movieId).document(commentId).collection("likes").document(uid)
        if (currentlyLiked) {
            doc.delete().await()
        } else {
            doc.set(mapOf("userId" to uid, "createdAt" to FieldValue.serverTimestamp())).await()
        }
    }
}
