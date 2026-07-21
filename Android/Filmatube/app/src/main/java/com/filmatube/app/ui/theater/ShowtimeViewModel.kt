package com.filmatube.app.ui.theater

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.theater.ChatRateLimitedException
import com.filmatube.app.data.theater.Showtime
import com.filmatube.app.data.theater.TheaterAttendance
import com.filmatube.app.data.theater.TheaterAttendee
import com.filmatube.app.data.theater.TheaterMessage
import com.filmatube.app.data.theater.TheaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * One showtime, from "interested" through the lobby to the door of the room.
 *
 * Detail (Day 156) and lobby (Day 157) are the same destination rather than two screens:
 * the showtime's own status decides whether you see an RSVP button, a countdown with
 * pre-show chat, or the way in. Splitting them would have meant two routes racing the
 * same status field, and a navigation jump the moment the doors opened under you.
 */
@HiltViewModel
class ShowtimeViewModel @Inject constructor(
    private val theaterRepository: TheaterRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val showtimeId: String = savedStateHandle["showtimeId"] ?: ""
    val myUid: String? get() = theaterRepository.myUid

    val showtime: StateFlow<Showtime?> = theaterRepository.observeShowtime(showtimeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * True once a snapshot has actually arrived. Without it a null showtime is ambiguous —
     * still loading, or deleted — and the screen would show "this showing is gone" for a
     * moment on every open.
     */
    val loaded: StateFlow<Boolean> = theaterRepository.observeShowtime(showtimeId)
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val attendance: StateFlow<TheaterAttendance> = theaterRepository.observeMyAttendance(showtimeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TheaterAttendance())

    /** Faces for the lobby — resolved, so capped at a screenful. */
    val attendees: StateFlow<List<TheaterAttendee>> = theaterRepository.observeAttendees(showtimeId, limit = 24)
        .map { theaterRepository.resolveAttendees(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val messages: StateFlow<List<TheaterMessage>> = theaterRepository.observeMessages(showtimeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Who is in the room right now, read off the showtime doc we already watch.
     *
     * Subscribing to the presence subcollection instead would deliver every viewer's
     * heartbeat to every viewer — O(N^2) reads, which a full room cannot afford.
     */
    val presentCount: StateFlow<Int> = showtime
        .map { it?.presentCount ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _spoiler = MutableStateFlow(false)
    val spoiler: StateFlow<Boolean> = _spoiler.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    /** Set when a send was throttled, so the composer can say so instead of failing silently. */
    private val _rateLimitedForMs = MutableStateFlow<Long?>(null)
    val rateLimitedForMs: StateFlow<Long?> = _rateLimitedForMs.asStateFlow()

    fun setDraft(value: String) { _draft.value = value }
    fun setSpoiler(value: Boolean) { _spoiler.value = value }
    fun clearRateLimited() { _rateLimitedForMs.value = null }

    // ── RSVP ──────────────────────────────────────────────────────────────

    fun toggleRsvp() {
        val current = attendance.value
        // Either state counts as "in" — tapping again should get you out of both.
        val isIn = current.going || current.waitlisted
        val full = showtime.value?.isFull == true

        viewModelScope.launch {
            _busy.value = true
            // Opting in turns reminders on by default; that's the point of the RSVP.
            theaterRepository.setRsvp(showtimeId, going = !isIn, remind = true, full = full)
            _busy.value = false
        }
    }

    fun toggleRemind() {
        val next = !attendance.value.remind
        viewModelScope.launch { theaterRepository.setRemind(showtimeId, next) }
    }

    // ── chat ──────────────────────────────────────────────────────────────

    fun send() {
        val text = _draft.value.trim()
        if (text.isBlank()) return
        val isSpoiler = _spoiler.value
        viewModelScope.launch {
            try {
                theaterRepository.sendMessage(showtimeId, text, isSpoiler)
                _draft.value = ""
                _spoiler.value = false
            } catch (e: ChatRateLimitedException) {
                // Keep the draft — the user should be able to resend, not retype.
                _rateLimitedForMs.value = e.retryInMs
            }
        }
    }

    /** Flag someone else's chat line for moderation. */
    fun report(message: TheaterMessage) {
        viewModelScope.launch { theaterRepository.reportMessage(showtimeId, message) }
    }

    fun react(emoji: String) {
        viewModelScope.launch { theaterRepository.sendReaction(showtimeId, emoji) }
    }
}
