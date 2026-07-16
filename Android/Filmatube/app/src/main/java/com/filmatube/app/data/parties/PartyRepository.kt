package com.filmatube.app.data.parties

import com.filmatube.app.data.boards.BoardRepository
import com.filmatube.app.data.social.FollowRepository
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

/** Lifecycle states of a watch party. */
object PartyStatus {
    const val SCHEDULED = "scheduled"
    const val LIVE = "live"
    const val ENDED = "ended"
}

/** A private watch party: one movie, one host, invited guests. */
data class Party(
    val id: String,
    val movieId: String,
    val movieTitle: String,
    val moviePoster: String,
    val hostId: String,
    val hostName: String,
    val status: String,
    val scheduledAtMs: Long,
    val memberCount: Int,
    val createdAtMs: Long,
) {
    val isLive: Boolean get() = status == PartyStatus.LIVE
    val isEnded: Boolean get() = status == PartyStatus.ENDED
}

/** A party guest (or the host). */
data class PartyMember(
    val uid: String,
    val name: String,
    val avatar: String,
    val role: String,
)

/**
 * The shared playback state — the heart of the sync engine. The HOST is the single
 * writer (enforced by rules); guests derive their expected position as
 * `positionMs + (now - updatedAtMs)` while playing, so one tiny document keeps
 * every player aligned without per-frame traffic.
 */
data class PartySyncState(
    val positionMs: Long,
    val isPlaying: Boolean,
    val updatedAtMs: Long,
) {
    /** Where the host's playhead should be *right now*, extrapolated from the last write. */
    fun expectedPositionMs(nowMs: Long = System.currentTimeMillis()): Long =
        if (isPlaying) positionMs + (nowMs - updatedAtMs).coerceAtLeast(0) else positionMs
}

/**
 * Watch parties: creation, membership, invites and the host→guests playback sync.
 * Schema mirrors boards (memberIds array + members subcollection) so the same
 * rule patterns apply; `sync/state` is a single doc only the host may write.
 */
@Singleton
class PartyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val boardRepository: BoardRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val parties get() = firestore.collection("parties")
    val myUid: String? get() = auth.currentUser?.uid

    // ── creation & lifecycle ──────────────────────────────────────────────

    /** Create a party for [movieId] at [scheduledAtMs]; host becomes the first member. */
    suspend fun createParty(
        movieId: String,
        movieTitle: String,
        moviePoster: String,
        scheduledAtMs: Long,
    ): String? = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext null
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        val ref = parties.document()
        runCatching {
            ref.set(
                mapOf(
                    "movieId" to movieId,
                    "movieTitle" to movieTitle,
                    "moviePoster" to moviePoster,
                    "hostId" to uid,
                    "hostName" to (me?.displayName ?: ""),
                    "status" to PartyStatus.SCHEDULED,
                    "scheduledAt" to com.google.firebase.Timestamp(scheduledAtMs / 1000, 0),
                    "memberIds" to listOf(uid),
                    "memberCount" to 1,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            ref.collection("members").document(uid).set(
                mapOf("userId" to uid, "role" to "host", "joinedAt" to FieldValue.serverTimestamp()),
            ).await()
        }.getOrNull() ?: return@withContext null
        ref.id
    }

    /** Host only: flip the party live (also resets the sync state to the start, paused). */
    suspend fun startParty(partyId: String) = withContext(ioDispatcher) {
        runCatching {
            parties.document(partyId).update("status", PartyStatus.LIVE).await()
            syncDoc(partyId).set(
                mapOf("positionMs" to 0L, "isPlaying" to false, "updatedAt" to FieldValue.serverTimestamp()),
            ).await()
        }
    }

    /** Host only: end the party. */
    suspend fun endParty(partyId: String) = withContext(ioDispatcher) {
        runCatching { parties.document(partyId).update("status", PartyStatus.ENDED).await() }
    }

    // ── reads ─────────────────────────────────────────────────────────────

    fun observeParty(partyId: String): Flow<Party?> = callbackFlow {
        val registration = parties.document(partyId).addSnapshotListener { snap, _ ->
            trySend(
                snap?.takeIf { it.exists() }?.let { d ->
                    Party(
                        id = d.id,
                        movieId = d.getString("movieId") ?: "",
                        movieTitle = d.getString("movieTitle") ?: "",
                        moviePoster = d.getString("moviePoster") ?: "",
                        hostId = d.getString("hostId") ?: "",
                        hostName = d.getString("hostName") ?: "",
                        status = d.getString("status") ?: PartyStatus.SCHEDULED,
                        scheduledAtMs = d.getTimestamp("scheduledAt")?.toDate()?.time ?: 0L,
                        memberCount = (d.getLong("memberCount") ?: 0L).toInt(),
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }

    /** Parties I host or joined that aren't over, soonest first. */
    fun observeMyParties(): Flow<List<Party>> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val registration = parties
            .whereArrayContains("memberIds", uid)
            .orderBy("scheduledAt", Query.Direction.ASCENDING)
            .limit(20)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { d ->
                    val status = d.getString("status") ?: return@mapNotNull null
                    if (status == PartyStatus.ENDED) return@mapNotNull null
                    Party(
                        id = d.id,
                        movieId = d.getString("movieId") ?: "",
                        movieTitle = d.getString("movieTitle") ?: "",
                        moviePoster = d.getString("moviePoster") ?: "",
                        hostId = d.getString("hostId") ?: "",
                        hostName = d.getString("hostName") ?: "",
                        status = status,
                        scheduledAtMs = d.getTimestamp("scheduledAt")?.toDate()?.time ?: 0L,
                        memberCount = (d.getLong("memberCount") ?: 0L).toInt(),
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    )
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    fun observeMembers(partyId: String): Flow<List<PartyMember>> = callbackFlow {
        val registration = parties.document(partyId).collection("members")
            .addSnapshotListener { snap, _ ->
                val members = snap?.documents ?: emptyList()
                trySend(members.map { d ->
                    PartyMember(
                        uid = d.id,
                        name = "",
                        avatar = "",
                        role = d.getString("role") ?: "guest",
                    )
                })
            }
        awaitClose { registration.remove() }
    }

    /** Resolve member display data (member docs hold only uid/role, like boards). */
    suspend fun resolveMember(member: PartyMember): PartyMember = withContext(ioDispatcher) {
        val user = runCatching { userRepository.getUser(member.uid) }.getOrNull()
        member.copy(name = user?.displayName ?: "", avatar = user?.avatarUrl ?: "")
    }

    fun observeMembership(partyId: String): Flow<Boolean> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val registration = parties.document(partyId).collection("members").document(uid)
            .addSnapshotListener { snap, _ -> trySend(snap?.exists() == true) }
        awaitClose { registration.remove() }
    }

    // ── membership ────────────────────────────────────────────────────────

    /** Join (accept an invite): membership-only party update + member doc, like boards. */
    suspend fun joinParty(partyId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val doc = parties.document(partyId)
        val batch = firestore.batch()
        batch.update(doc, mapOf("memberIds" to FieldValue.arrayUnion(uid), "memberCount" to FieldValue.increment(1)))
        batch.set(
            doc.collection("members").document(uid),
            mapOf("userId" to uid, "role" to "guest", "joinedAt" to FieldValue.serverTimestamp()),
        )
        runCatching { batch.commit().await() }
    }

    /** Leave (hosts can't leave their own party — they end it instead). */
    suspend fun leaveParty(partyId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val doc = parties.document(partyId)
        val hostId = runCatching { doc.get().await().getString("hostId") }.getOrNull()
        if (hostId == uid) return@withContext
        val batch = firestore.batch()
        batch.update(doc, mapOf("memberIds" to FieldValue.arrayRemove(uid), "memberCount" to FieldValue.increment(-1)))
        batch.delete(doc.collection("members").document(uid))
        runCatching { batch.commit().await() }
    }

    // ── invites (in-app inbox; FCM push arrives with the notifications Cloud Function) ──

    private suspend fun invite(partyId: String, party: Party, toUid: String): Boolean {
        val uid = myUid ?: return false
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        return runCatching {
            firestore.collection("users").document(toUid).collection("notifications").add(
                mapOf(
                    "type" to "party_invite",
                    "actorId" to uid,
                    "actorName" to (me?.displayName ?: ""),
                    "actorAvatar" to (me?.avatarUrl ?: ""),
                    "partyId" to partyId,
                    "movieId" to party.movieId,
                    "movieTitle" to party.movieTitle,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }.isSuccess
    }

    /** Invite all of my followers. Returns how many invites were written. */
    suspend fun inviteFollowers(partyId: String, party: Party): Int = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext 0
        val followerIds = runCatching { followRepository.observeFollowerIds(uid).first() }.getOrDefault(emptyList())
        followerIds.count { it != uid && invite(partyId, party, it) }
    }

    /** Invite every member of one of my boards (minus people already in the party). */
    suspend fun inviteBoardMembers(partyId: String, party: Party, boardId: String): Int = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext 0
        val memberIds = runCatching {
            firestore.collection("boards").document(boardId).get().await()
                .get("memberIds") as? List<*>
        }.getOrNull()?.filterIsInstance<String>() ?: emptyList()
        val partyMemberIds = runCatching {
            parties.document(partyId).get().await().get("memberIds") as? List<*>
        }.getOrNull()?.filterIsInstance<String>() ?: emptyList()
        memberIds.count { it != uid && it !in partyMemberIds && invite(partyId, party, it) }
    }

    /** My boards, for the invite picker. */
    fun observeMyBoards() = boardRepository.observeMyBoards()

    // ── sync engine ───────────────────────────────────────────────────────

    private fun syncDoc(partyId: String) = parties.document(partyId).collection("sync").document("state")

    /**
     * HOST: publish the authoritative playback state. Called on play/pause/seek and as a
     * ~5s heartbeat while playing — guests extrapolate between writes, so this stays cheap.
     */
    suspend fun publishSync(partyId: String, positionMs: Long, isPlaying: Boolean) = withContext(ioDispatcher) {
        runCatching {
            syncDoc(partyId).set(
                mapOf(
                    "positionMs" to positionMs,
                    "isPlaying" to isPlaying,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    /** GUESTS: the host's latest state, realtime. */
    fun observeSync(partyId: String): Flow<PartySyncState?> = callbackFlow {
        val registration = syncDoc(partyId).addSnapshotListener { snap, _ ->
            if (snap == null || !snap.exists()) {
                trySend(null)
                return@addSnapshotListener
            }
            // Pending server timestamps read as null — fall back to local now so
            // extrapolation still works while the write is in flight.
            val updated = snap.getTimestamp("updatedAt")?.toDate()?.time ?: System.currentTimeMillis()
            trySend(
                PartySyncState(
                    positionMs = snap.getLong("positionMs") ?: 0L,
                    isPlaying = snap.getBoolean("isPlaying") ?: false,
                    updatedAtMs = updated,
                ),
            )
        }
        awaitClose { registration.remove() }
    }
}
