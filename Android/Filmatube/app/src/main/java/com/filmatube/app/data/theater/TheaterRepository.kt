package com.filmatube.app.data.theater

import com.filmatube.app.di.IoDispatcher
import com.filmatube.app.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lifecycle states of a theater showtime (admin/automation driven, never client written). */
object ShowtimeStatus {
    const val SCHEDULED = "scheduled"
    const val LOBBY = "lobby"
    const val LIVE = "live"
    const val ENDED = "ended"

    /** Everything the Theater tab lists — anything not yet over. */
    val OPEN = listOf(SCHEDULED, LOBBY, LIVE)
}

/**
 * One public screening on the theater lineup.
 *
 * Unlike a watch party, a showtime has no host-member relationship: it is scheduled by
 * admins, open to everyone, and driven by a server clock (v1.2). Day 155 only reads it.
 */
data class Showtime(
    val id: String,
    val movieId: String,
    val movieTitle: String,
    val posterUrl: String,
    val backdropUrl: String,
    val startAtMs: Long,
    val status: String,
    val isPremiere: Boolean,
    val capacity: Int,
    val attendeesCount: Int,
    /**
     * Non-zero while an admin has the showing paused; the effective clock freezes here.
     *
     * The theater has no host, so a pause can't be "somebody's player stopped" — it's an
     * edit to the schedule itself, written by the admin console (web Day 167).
     */
    val pausedAtMs: Long = 0L,
    /** Non-empty when the screening is private to one board — only members may see or join. */
    val boardId: String = "",
    /** How many people are queued for a seat once the room is full. */
    val waitlistCount: Int = 0,
    /**
     * How many people are in the room right now, maintained server-side.
     *
     * Denormalized deliberately: counting by subscribing to the presence subcollection meant
     * every viewer received every other viewer heartbeat — O(N^2) document delivery, which
     * at a thousand viewers is roughly a million reads every 30 seconds.
     */
    val presentCount: Int = 0,
) {
    val isPaused: Boolean get() = pausedAtMs > 0L
    val isLive: Boolean get() = status == ShowtimeStatus.LIVE

    /** Doors are open: the lobby is up, or the film is already rolling. */
    val isOpen: Boolean get() = status == ShowtimeStatus.LOBBY || status == ShowtimeStatus.LIVE

    /** 0 capacity means unlimited, so a sold-out room needs an explicit cap. */
    val isFull: Boolean get() = capacity > 0 && attendeesCount >= capacity

    fun startsInMs(nowMs: Long): Long = (startAtMs - nowMs).coerceAtLeast(0L)

    /**
     * Where the film should be, for everyone, right now.
     *
     * This is the whole sync model: a public screening runs on the wall clock, so position
     * is simply "time since the doors opened". Nobody publishes a playhead, there is no host
     * to drift from, and joining late lands you exactly where the room already is — catch-up
     * falls out for free. [serverNowMs] must be server time, not device time.
     *
     * An admin pause freezes the effective clock at [pausedAtMs]; resuming shifts [startAtMs]
     * forward by the paused duration, which is why this stays a pure function of the two
     * fields with no accumulated pause bookkeeping to drift out of step.
     */
    fun playbackPositionMs(serverNowMs: Long): Long {
        val effectiveNow = if (isPaused) pausedAtMs else serverNowMs
        return (effectiveNow - startAtMs).coerceAtLeast(0L)
    }
}

/** Somebody who RSVP'd to a showtime, as shown in the card's avatar stack. */
data class TheaterAttendee(
    val uid: String,
    val name: String = "",
    val avatar: String = "",
)

/** My own attendance record for a showtime. */
data class TheaterAttendance(
    val going: Boolean = false,
    val remind: Boolean = false,
    /** Queued for a seat because the room was full when I asked. */
    val waitlisted: Boolean = false,
)

/**
 * A heartbeat from somebody currently in the room. Presence is deliberately separate from
 * an RSVP: one is "I'm here now", the other is "I plan to come".
 */
data class TheaterPresence(
    val uid: String,
    val presentAtMs: Long,
) {
    fun isFresh(nowMs: Long): Boolean = nowMs - presentAtMs < PRESENCE_STALE_AFTER_MS
}

/** How long after its last heartbeat a presence record stops counting as "in the room". */
const val PRESENCE_STALE_AFTER_MS = 90_000L

/** How often a viewer re-stamps their presence while watching. */
const val PRESENCE_HEARTBEAT_MS = 30_000L

/** Emoji available as floating reactions in the theater (same set as watch parties). */
val THEATER_REACTIONS = listOf("😂", "😮", "❤️", "🔥", "😢", "👏")

/** A line in the lobby / live chat. */
data class TheaterMessage(
    val id: String,
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val isSpoiler: Boolean,
    val createdAtMs: Long,
    val isMine: Boolean,
)

/** An ephemeral floating emoji during a live showing. */
data class TheaterReaction(
    val id: String,
    val emoji: String,
    val userName: String,
    val createdAtMs: Long,
)

/** Sending too fast — the composer surfaces the wait so it doesn't just swallow the line. */
class ChatRateLimitedException(val retryInMs: Long) : Exception()

/**
 * The online movie theater lineup.
 *
 * Reads only — showtimes are written by admins (Day 166) and advanced through their
 * lifecycle by a Cloud Function (Day 170); Firestore rules already enforce that.
 */
@Singleton
class TheaterRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val showtimes get() = firestore.collection("showtimes")
    val myUid: String? get() = auth.currentUser?.uid

    /**
     * The whole open lineup, soonest first: live and lobby rooms plus everything scheduled.
     *
     * One query rather than one per status — `whereIn` on status with `orderBy(startAt)`
     * rides the existing (status, startAt) composite index, and the UI partitions the
     * result into "now showing" and "upcoming" client-side.
     */
    fun observeLineup(limit: Long = 50): Flow<List<Showtime>> = callbackFlow {
        val registration = showtimes
            // Public only. Board-private screenings are reached from their board, and the
            // read rule rejects a query that could return one rather than filtering it out.
            .whereEqualTo("boardId", "")
            .whereIn("status", ShowtimeStatus.OPEN)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { d ->
                    Showtime(
                        id = d.id,
                        movieId = d.getString("movieId") ?: "",
                        movieTitle = d.getString("movieTitle") ?: "",
                        posterUrl = d.getString("posterUrl") ?: "",
                        backdropUrl = d.getString("backdropUrl") ?: "",
                        startAtMs = d.getTimestamp("startAt")?.toDate()?.time ?: 0L,
                        status = d.getString("status") ?: ShowtimeStatus.SCHEDULED,
                        isPremiere = d.getBoolean("isPremiere") ?: false,
                        capacity = (d.getLong("capacity") ?: 0L).toInt(),
                        attendeesCount = (d.getLong("attendeesCount") ?: 0L).toInt(),
                        pausedAtMs = d.getTimestamp("pausedAt")?.toDate()?.time ?: 0L,
                        boardId = d.getString("boardId") ?: "",
                        waitlistCount = (d.getLong("waitlistCount") ?: 0L).toInt(),
                        presentCount = (d.getLong("presentCount") ?: 0L).toInt(),
                    )
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** A single showtime, realtime (detail screen lands on Day 156). */
    fun observeShowtime(showtimeId: String): Flow<Showtime?> = callbackFlow {
        val registration = showtimes.document(showtimeId).addSnapshotListener { snap, _ ->
            trySend(
                snap?.takeIf { it.exists() }?.let { d ->
                    Showtime(
                        id = d.id,
                        movieId = d.getString("movieId") ?: "",
                        movieTitle = d.getString("movieTitle") ?: "",
                        posterUrl = d.getString("posterUrl") ?: "",
                        backdropUrl = d.getString("backdropUrl") ?: "",
                        startAtMs = d.getTimestamp("startAt")?.toDate()?.time ?: 0L,
                        status = d.getString("status") ?: ShowtimeStatus.SCHEDULED,
                        isPremiere = d.getBoolean("isPremiere") ?: false,
                        capacity = (d.getLong("capacity") ?: 0L).toInt(),
                        attendeesCount = (d.getLong("attendeesCount") ?: 0L).toInt(),
                        pausedAtMs = d.getTimestamp("pausedAt")?.toDate()?.time ?: 0L,
                        boardId = d.getString("boardId") ?: "",
                        waitlistCount = (d.getLong("waitlistCount") ?: 0L).toInt(),
                        presentCount = (d.getLong("presentCount") ?: 0L).toInt(),
                    )
                },
            )
        }
        awaitClose { registration.remove() }
    }

    /**
     * The faces shown on the featured card — a handful of attendees, not the full room.
     * Attendee docs hold only rsvp/joinedAt (like board members), so names and avatars
     * are resolved from `users` here.
     */
    fun observeAttendees(showtimeId: String, limit: Long = 6): Flow<List<TheaterAttendee>> = callbackFlow {
        val registration = showtimes.document(showtimeId).collection("attendees")
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { TheaterAttendee(uid = it.id) } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    /** Fill in display data for the avatar stack. */
    suspend fun resolveAttendees(attendees: List<TheaterAttendee>): List<TheaterAttendee> =
        withContext(ioDispatcher) {
            attendees.map { attendee ->
                val user = runCatching { userRepository.getUser(attendee.uid) }.getOrNull()
                attendee.copy(name = user?.displayName ?: "", avatar = user?.avatarUrl ?: "")
            }
        }

    // ── RSVP & reminders (Day 156) ────────────────────────────────────────

    private fun attendeeDoc(showtimeId: String, uid: String) =
        showtimes.document(showtimeId).collection("attendees").document(uid)

    private fun waitlistDoc(showtimeId: String, uid: String) =
        showtimes.document(showtimeId).collection("waitlist").document(uid)

    /**
     * RSVP to a showtime, or cancel it. Returns true when the seat went to a waitlist.
     *
     * Only the attendee doc is touched: `attendeesCount` on the showtime is maintained by
     * the `syncShowtimeAttendees` Cloud Function, because rules keep showtime docs
     * admin-writable. (Boards let clients bump their own memberCount; a public theater is
     * a bigger spoofing target, so the count is server-owned here.)
     *
     * A full room queues instead of refusing — a sold-out premiere is exactly when someone
     * most wants telling that a seat opened, and `promoteFromWaitlist` pulls them in.
     */
    suspend fun setRsvp(
        showtimeId: String,
        going: Boolean,
        remind: Boolean = true,
        full: Boolean = false,
    ): Boolean = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext false
        if (!going) {
            // Leaving frees a seat, so drop out of both lists.
            runCatching { attendeeDoc(showtimeId, uid).delete().await() }
            runCatching { waitlistDoc(showtimeId, uid).delete().await() }
            return@withContext false
        }

        val alreadySeated = runCatching { attendeeDoc(showtimeId, uid).get().await().exists() }
            .getOrDefault(false)
        if (full && !alreadySeated) {
            runCatching {
                waitlistDoc(showtimeId, uid).set(
                    mapOf("userId" to uid, "joinedAt" to FieldValue.serverTimestamp()),
                ).await()
            }
            return@withContext true
        }

        runCatching {
            attendeeDoc(showtimeId, uid).set(
                mapOf(
                    "rsvp" to true,
                    "remind" to remind,
                    "joinedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        false
    }

    /** Toggle just the "remind me" flag, leaving the RSVP itself alone. */
    suspend fun setRemind(showtimeId: String, remind: Boolean) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        runCatching {
            attendeeDoc(showtimeId, uid).set(mapOf("remind" to remind), SetOptions.merge()).await()
        }
    }

    /**
     * My RSVP state for a showtime, realtime — including whether I'm queued.
     *
     * Two listeners rather than one: being promoted off the waitlist is a server write to a
     * *different* document, so watching only the attendee doc would leave someone staring at
     * "you're on the waitlist" after a seat had already been given to them.
     */
    fun observeMyAttendance(showtimeId: String): Flow<TheaterAttendance> = callbackFlow {
        val uid = myUid
        if (uid == null) {
            trySend(TheaterAttendance())
            awaitClose { }
            return@callbackFlow
        }

        var seated = false
        var queued = false
        var reminding = false
        fun emit() = trySend(TheaterAttendance(going = seated, remind = reminding, waitlisted = queued))

        val attendee = attendeeDoc(showtimeId, uid).addSnapshotListener { snap, _ ->
            seated = snap?.exists() == true && (snap.getBoolean("rsvp") ?: false)
            reminding = snap?.getBoolean("remind") ?: false
            emit()
        }
        val waitlist = waitlistDoc(showtimeId, uid).addSnapshotListener { snap, _ ->
            queued = snap?.exists() == true
            emit()
        }
        awaitClose { attendee.remove(); waitlist.remove() }
    }

    // ── presence (Day 160) ────────────────────────────────────────────────

    private fun presence(showtimeId: String) = showtimes.document(showtimeId).collection("presence")

    /**
     * Heartbeat: I am in this room right now.
     *
     * Presence is its own subcollection rather than a flag on the attendee doc, because the
     * two answer different questions — an RSVP is intent ("I plan to come"), presence is
     * fact ("I'm here"). Folding them together would also mean walking into a room silently
     * inflated `attendeesCount`, which is the RSVP number people see before it starts.
     */
    suspend fun markPresent(showtimeId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        runCatching {
            presence(showtimeId).document(uid).set(
                mapOf("userId" to uid, "presentAt" to FieldValue.serverTimestamp()),
            ).await()
        }
    }

    /** Leaving the room. Best-effort: a killed app is reaped by the staleness window instead. */
    suspend fun clearPresence(showtimeId: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        runCatching { presence(showtimeId).document(uid).delete().await() }
    }

    /**
     * Who is in the room, with their last heartbeat.
     *
     * Freshness is judged by the caller against [PRESENCE_STALE_AFTER_MS] rather than by a
     * `whereGreaterThan` on the query: the cutoff moves every second, and a realtime listener
     * with a moving bound would have to be torn down and rebuilt constantly. Capped at
     * [limit] — a sold-out premiere shows a count from the cap, not the true total.
     */
    fun observePresence(showtimeId: String, limit: Long = 100): Flow<List<TheaterPresence>> = callbackFlow {
        val registration = presence(showtimeId)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { d ->
                    TheaterPresence(
                        uid = d.id,
                        presentAtMs = d.getTimestamp("presentAt")?.toDate()?.time ?: 0L,
                    )
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }

    // ── server clock (Day 158) ────────────────────────────────────────────

    /**
     * How far this device's clock is behind the server, in ms (positive = device is slow).
     *
     * The theater is driven by wall-clock time rather than a host's playhead, so a device
     * with a skewed clock would sit at the wrong point in the film with nothing to correct
     * it. We measure the skew by stamping a server timestamp into our own attendee doc and
     * comparing it with the local time captured around the write. Round-trip latency leaks
     * in, but the error is far below the seek tolerance we care about.
     */
    suspend fun measureServerClockOffset(): Long = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext 0L
        // Probed against a scratch doc under my own user, not my attendee doc: writing to
        // the latter would create attendance as a side effect of merely opening the room,
        // silently inflating attendeesCount via the sync function.
        val doc = firestore.collection("users").document(uid)
            .collection("settings").document("clockProbe")
        runCatching {
            val before = System.currentTimeMillis()
            doc.set(mapOf("clockProbeAt" to FieldValue.serverTimestamp()), SetOptions.merge()).await()
            val after = System.currentTimeMillis()
            // Read from the server, not the local cache, or we'd read back our own pending write.
            val serverMs = doc.get(Source.SERVER).await()
                .getTimestamp("clockProbeAt")?.toDate()?.time
                ?: return@runCatching 0L
            // The write landed somewhere inside [before, after]; midpoint is the best estimate.
            serverMs - (before + after) / 2
        }.getOrDefault(0L)
    }

    // ── chat (Days 157 & 159) ─────────────────────────────────────────────

    private fun chat(showtimeId: String) = showtimes.document(showtimeId).collection("chat")

    /** Minimum gap between two messages from the same device. */
    private val chatIntervalMs = 2_000L
    private var lastChatSentAtMs = 0L

    /**
     * Post a lobby / live chat line.
     *
     * Rate limiting is enforced here rather than only at the composer so every caller gets
     * it. This is a courtesy throttle, not a security control — a modified client can still
     * post at will, so the server-side limit lands with the theater automation on Day 170.
     */
    suspend fun sendMessage(showtimeId: String, text: String, isSpoiler: Boolean = false) =
        withContext(ioDispatcher) {
            val uid = myUid ?: return@withContext
            if (text.isBlank()) return@withContext

            val now = System.currentTimeMillis()
            val elapsed = now - lastChatSentAtMs
            if (elapsed < chatIntervalMs) throw ChatRateLimitedException(chatIntervalMs - elapsed)
            lastChatSentAtMs = now

            val me = runCatching { userRepository.getUser(uid) }.getOrNull()
            runCatching {
                chat(showtimeId).add(
                    mapOf(
                        "userId" to uid,
                        "userName" to (me?.displayName ?: ""),
                        "userAvatar" to (me?.avatarUrl ?: ""),
                        "text" to text.trim().take(200),
                        "isSpoiler" to isSpoiler,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            }.onFailure {
                // The write failed, so don't hold the user to a throttle they never used.
                lastChatSentAtMs = 0L
            }
        }

    /** Newest chat lines, oldest-first for the list. */
    fun observeMessages(showtimeId: String, limit: Long = 100): Flow<List<TheaterMessage>> = callbackFlow {
        val uid = myUid
        val registration = chat(showtimeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.map { d ->
                    TheaterMessage(
                        id = d.id,
                        userId = d.getString("userId") ?: "",
                        userName = d.getString("userName") ?: "",
                        userAvatar = d.getString("userAvatar") ?: "",
                        text = d.getString("text") ?: "",
                        isSpoiler = d.getBoolean("isSpoiler") ?: false,
                        // Pending server timestamps read null — treat as "just now" so my own
                        // line doesn't jump position once the write lands.
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                        isMine = uid != null && d.getString("userId") == uid,
                    )
                }?.reversed() ?: emptyList()
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    // ── floating reactions (Day 159) ──────────────────────────────────────

    private fun reactions(showtimeId: String) = showtimes.document(showtimeId).collection("reactions")

    /** Fire a floating emoji everyone in the room sees. */
    suspend fun sendReaction(showtimeId: String, emoji: String) = withContext(ioDispatcher) {
        val uid = myUid ?: return@withContext
        val me = runCatching { userRepository.getUser(uid) }.getOrNull()
        runCatching {
            reactions(showtimeId).add(
                mapOf(
                    "userId" to uid,
                    "userName" to (me?.displayName ?: ""),
                    "emoji" to emoji,
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    /** Recent reactions; the overlay animates each one then drops it. */
    fun observeReactions(showtimeId: String, limit: Long = 20): Flow<List<TheaterReaction>> = callbackFlow {
        val registration = reactions(showtimeId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.documents?.map { d ->
                    TheaterReaction(
                        id = d.id,
                        emoji = d.getString("emoji") ?: "",
                        userName = d.getString("userName") ?: "",
                        createdAtMs = d.getTimestamp("createdAt")?.toDate()?.time ?: System.currentTimeMillis(),
                    )
                } ?: emptyList())
            }
        awaitClose { registration.remove() }
    }
}
