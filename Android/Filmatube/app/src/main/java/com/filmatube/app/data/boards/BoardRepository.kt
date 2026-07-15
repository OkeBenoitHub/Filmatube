package com.filmatube.app.data.boards

import com.filmatube.app.data.social.FollowRepository
import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** The two kinds of board. */
object BoardTypes {
    const val MOVIE = "movie"
    const val GENERAL = "general"
}

/** A chat message in a board. */
data class BoardMessage(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val hasSpoiler: Boolean,
    val createdAtMs: Long,
    val isMine: Boolean,
)

/** A community board (discussion space). */
data class Board(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val type: String,
    val movieId: String,
    val movieTitle: String,
    val isPublic: Boolean,
    val isFeatured: Boolean,
    val isOfficial: Boolean,
    val ownerId: String,
    val memberCount: Int,
    val createdAtMs: Long,
)

/** Board discovery: featured + browse by type, reading public `boards`. */
@Singleton
class BoardRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val boards get() = firestore.collection("boards")
    val myUid: String? get() = auth.currentUser?.uid

    /** Featured public boards for the discovery header. */
    fun observeFeatured(limit: Long = 10): Flow<List<Board>> = callbackFlow {
        val registration = boards
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isFeatured", true)
            .orderBy("memberCount", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ -> trySend(snap.toBoards()) }
        awaitClose { registration.remove() }
    }

    /** Public boards, optionally filtered by [type] (movie/general), most-popular first. */
    fun observeBoards(type: String? = null, limit: Long = 50): Flow<List<Board>> = callbackFlow {
        var query: Query = boards.whereEqualTo("isPublic", true)
        if (type != null) query = query.whereEqualTo("type", type)
        val registration = query
            .orderBy("memberCount", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ -> trySend(snap.toBoards()) }
        awaitClose { registration.remove() }
    }

    /** Create a board owned by the current user (added as first member). Returns the new id. */
    suspend fun createBoard(
        title: String,
        description: String,
        type: String,
        isPublic: Boolean,
        coverUrl: String,
    ): String? = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext null
        val ref = boards.document()
        ref.set(
            mapOf(
                "title" to title.trim(),
                "description" to description.trim(),
                "type" to type,
                "coverUrl" to coverUrl,
                "isPublic" to isPublic,
                "isFeatured" to false,
                "isOfficial" to false,
                "ownerId" to uid,
                "memberIds" to listOf(uid),
                "memberCount" to 1,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        ref.collection("members").document(uid).set(
            mapOf("userId" to uid, "role" to "owner", "joinedAt" to FieldValue.serverTimestamp()),
        ).await()
        ref.id
    }

    /** Boards the current user owns or has joined, newest first. */
    fun observeMyBoards(): Flow<List<Board>> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = boards
            .whereArrayContains("memberIds", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ -> trySend(snap.toBoards()) }
        awaitClose { registration.remove() }
    }

    /** Whether the current user is a member of [boardId] (realtime). */
    fun observeMembership(boardId: String): Flow<Boolean> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val registration = boards.document(boardId).collection("members").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { registration.remove() }
    }

    /** Join [boardId]: add self to members + memberIds and bump the count. */
    suspend fun joinBoard(boardId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val board = boards.document(boardId)
        val batch = firestore.batch()
        batch.update(board, mapOf("memberIds" to FieldValue.arrayUnion(uid), "memberCount" to FieldValue.increment(1)))
        batch.set(
            board.collection("members").document(uid),
            mapOf("userId" to uid, "role" to "member", "joinedAt" to FieldValue.serverTimestamp()),
        )
        runCatching { batch.commit().await() }
    }

    /** Leave [boardId] (owners can't leave their own board). */
    suspend fun leaveBoard(boardId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val doc = boards.document(boardId)
        val ownerId = runCatching { doc.get().await().getString("ownerId") }.getOrNull()
        if (ownerId == uid) return@withContext
        val batch = firestore.batch()
        batch.update(doc, mapOf("memberIds" to FieldValue.arrayRemove(uid), "memberCount" to FieldValue.increment(-1)))
        batch.delete(doc.collection("members").document(uid))
        runCatching { batch.commit().await() }
    }

    /** Invite the current user's followers to [boardId] — writes a notification into each inbox. */
    suspend fun inviteFollowers(boardId: String, boardTitle: String): Int = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext 0
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        val followerIds = runCatching { followRepository.observeFollowerIds(uid).first() }.getOrDefault(emptyList())
        var invited = 0
        for (followerId in followerIds) {
            val ok = runCatching {
                firestore.collection("users").document(followerId).collection("notifications").add(
                    mapOf(
                        "type" to "board_invite",
                        "actorId" to uid,
                        "actorName" to (me?.displayName ?: ""),
                        "actorAvatar" to (me?.avatarUrl ?: ""),
                        "boardId" to boardId,
                        "boardTitle" to boardTitle,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }.isSuccess
            if (ok) invited++
        }
        invited
    }

    // ── chat ──────────────────────────────────────────────────────────────
    private fun messages(boardId: String) = boards.document(boardId).collection("messages")
    private fun typing(boardId: String) = boards.document(boardId).collection("typing")

    /** Realtime messages for [boardId], oldest first (capped). */
    fun observeMessages(boardId: String, limit: Long = 100): Flow<List<BoardMessage>> = callbackFlow {
        val uid = myUid
        val registration = messages(boardId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { d ->
                    BoardMessage(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        userName = d.getString("userName") ?: "",
                        userAvatar = d.getString("userAvatar") ?: "",
                        text = d.getString("text") ?: "",
                        hasSpoiler = d.getBoolean("hasSpoiler") ?: false,
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        isMine = uid != null && d.getString("userId") == uid,
                    )
                }?.reversed() ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    suspend fun sendMessage(boardId: String, text: String, hasSpoiler: Boolean) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        if (text.isBlank()) return@withContext
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        runCatching {
            messages(boardId).add(
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
        runCatching { typing(boardId).document(uid).delete().await() }
    }

    suspend fun deleteMessage(boardId: String, messageId: String) = withContext(ioDispatcher) {
        runCatching { messages(boardId).document(messageId).delete().await() }
    }

    /** Mark the current user as typing (or clear it). */
    suspend fun setTyping(boardId: String, isTyping: Boolean) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val doc = typing(boardId).document(uid)
        if (isTyping) {
            val me = runCatching { userRepository.getUser(uid) }.getOrNull()
            runCatching {
                doc.set(mapOf("name" to (me?.displayName ?: ""), "updatedAt" to FieldValue.serverTimestamp())).await()
            }
        } else {
            runCatching { doc.delete().await() }
        }
    }

    /** Names of other users typing in the last 6 seconds. */
    fun observeTyping(boardId: String): Flow<List<String>> = callbackFlow {
        val uid = myUid
        val registration = typing(boardId).addSnapshotListener { snap, _ ->
            val now = System.currentTimeMillis()
            val names = snap?.documents.orEmpty()
                .filter { it.id != uid && (now - (it.getTimestamp("updatedAt")?.toDate()?.time ?: 0L)) < 6_000 }
                .mapNotNull { it.getString("name")?.ifBlank { null } }
            trySend(names)
        }
        awaitClose { registration.remove() }
    }

    /** Single board by id (for the board detail header). */
    suspend fun getBoard(id: String): Board? = withContext(ioDispatcher) {
        val doc = runCatching { boards.document(id).get().await() }.getOrNull()
        if (doc == null || !doc.exists()) null else doc.toBoard()
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toBoard(): Board = Board(
        id = id,
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        coverUrl = getString("coverUrl") ?: "",
        type = getString("type") ?: BoardTypes.GENERAL,
        movieId = getString("movieId") ?: "",
        movieTitle = getString("movieTitle") ?: "",
        isPublic = getBoolean("isPublic") ?: true,
        isFeatured = getBoolean("isFeatured") ?: false,
        isOfficial = getBoolean("isOfficial") ?: false,
        ownerId = getString("ownerId") ?: "",
        memberCount = (getLong("memberCount") ?: 0L).toInt(),
        createdAtMs = getTimestamp("createdAt")?.toDate()?.time ?: 0L,
    )

    private fun com.google.firebase.firestore.QuerySnapshot?.toBoards(): List<Board> =
        this?.documents?.map { d ->
            Board(
                id = d.id,
                title = d.getString("title") ?: "",
                description = d.getString("description") ?: "",
                coverUrl = d.getString("coverUrl") ?: "",
                type = d.getString("type") ?: BoardTypes.GENERAL,
                movieId = d.getString("movieId") ?: "",
                movieTitle = d.getString("movieTitle") ?: "",
                isPublic = d.getBoolean("isPublic") ?: true,
                isFeatured = d.getBoolean("isFeatured") ?: false,
                isOfficial = d.getBoolean("isOfficial") ?: false,
                ownerId = d.getString("ownerId") ?: "",
                memberCount = (d.getLong("memberCount") ?: 0L).toInt(),
                createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
            )
        } ?: emptyList()
}
