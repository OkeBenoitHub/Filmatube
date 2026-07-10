package com.filmatube.app.data.notifications

import com.filmatube.app.di.IoDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Notification categories → push channels + in-app grouping. */
object NotificationTypes {
    const val FOLLOW = "follow"
    const val RECOMMENDATION = "recommendation"
    const val REPLY = "reply"
    const val REVIEW_LIKE = "review_like"
    const val SYSTEM = "system"

    /** Push channel category for a given in-app notification type. */
    fun categoryFor(type: String): String = when (type) {
        SYSTEM -> "system"
        else -> "social"
    }
}

/** An in-app notification in the current user's inbox. */
data class AppNotification(
    val id: String,
    val type: String,
    val actorId: String,
    val actorName: String,
    val actorAvatar: String,
    val movieId: String,
    val movieTitle: String,
    val message: String,
    val read: Boolean,
    val createdAtMs: Long,
)

/**
 * FCM token registration + the in-app notification inbox at `users/{uid}/notifications`.
 * Delivery of actual push is done by a Cloud Function reading `users/{uid}/fcmTokens`
 * (Day 108/110); until then these inbox writes power the in-app notification center.
 */
@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val myUid: String? get() = auth.currentUser?.uid
    private fun inbox(uid: String) =
        firestore.collection("users").document(uid).collection("notifications")

    /** Persist this device's FCM token under the signed-in user (called after sign-in). */
    suspend fun registerToken() = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull() ?: return@withContext
        runCatching {
            firestore.collection("users").document(uid).collection("fcmTokens").document(token).set(
                mapOf(
                    "token" to token,
                    "platform" to "android",
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    /** Write a notification into [toUid]'s inbox (client-side fan-out; skipped for self). */
    suspend fun notify(
        toUid: String,
        type: String,
        actorName: String,
        actorAvatar: String,
        movieId: String = "",
        movieTitle: String = "",
        message: String = "",
    ) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        if (toUid == uid || toUid.isBlank()) return@withContext
        runCatching {
            inbox(toUid).add(
                mapOf(
                    "type" to type,
                    "actorId" to uid,
                    "actorName" to actorName,
                    "actorAvatar" to actorAvatar,
                    "movieId" to movieId,
                    "movieTitle" to movieTitle,
                    "message" to message,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    fun observeNotifications(limit: Long = 50): Flow<List<AppNotification>> {
        val uid = myUid ?: return flowOf(emptyList())
        return callbackFlow {
            val registration = inbox(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snap, _ ->
                    trySend(
                        snap?.documents?.map { d ->
                            AppNotification(
                                id = d.id,
                                type = d.getString("type") ?: "",
                                actorId = d.getString("actorId") ?: "",
                                actorName = d.getString("actorName") ?: "",
                                actorAvatar = d.getString("actorAvatar") ?: "",
                                movieId = d.getString("movieId") ?: "",
                                movieTitle = d.getString("movieTitle") ?: "",
                                message = d.getString("message") ?: "",
                                read = d.getBoolean("read") ?: false,
                                createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                            )
                        } ?: emptyList(),
                    )
                }
            awaitClose { registration.remove() }
        }
    }

    val unreadCount: Flow<Int> get() = observeNotifications().map { list -> list.count { !it.read } }

    suspend fun markRead(id: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        runCatching { inbox(uid).document(id).update("read", true).await() }
    }

    suspend fun markAllRead() = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val unread = runCatching { inbox(uid).whereEqualTo("read", false).get().await() }.getOrNull() ?: return@withContext
        val batch = firestore.batch()
        unread.documents.forEach { batch.update(it.reference, "read", true) }
        runCatching { batch.commit().await() }
    }
}
