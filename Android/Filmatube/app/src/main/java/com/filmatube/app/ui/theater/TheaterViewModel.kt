package com.filmatube.app.ui.theater

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.theater.Showtime
import com.filmatube.app.data.theater.TheaterAttendee
import com.filmatube.app.data.theater.TheaterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** The lineup, split the way the tab presents it. */
data class TheaterLineup(
    val nowShowing: List<Showtime> = emptyList(),
    val upcoming: List<Showtime> = emptyList(),
    val loaded: Boolean = false,
) {
    val isEmpty: Boolean get() = nowShowing.isEmpty() && upcoming.isEmpty()

    /** The card that gets the hero treatment: whatever is on now, else the next one up. */
    val featured: Showtime? get() = nowShowing.firstOrNull() ?: upcoming.firstOrNull()
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TheaterViewModel @Inject constructor(
    private val theaterRepository: TheaterRepository,
) : ViewModel() {

    val lineup: StateFlow<TheaterLineup> = theaterRepository.observeLineup()
        .map { showtimes ->
            TheaterLineup(
                nowShowing = showtimes.filter { it.isOpen },
                upcoming = showtimes.filterNot { it.isOpen },
                loaded = true,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TheaterLineup())

    /**
     * Attendee faces for the featured showtime only. Resolving names/avatars costs a
     * user read each, so the list rows settle for the count and just the hero gets faces.
     */
    val featuredAttendees: StateFlow<List<TheaterAttendee>> = lineup
        .map { it.featured?.id }
        .distinctUntilChanged()
        .flatMapLatest { showtimeId ->
            if (showtimeId == null) flowOf(emptyList()) else attendeesOf(showtimeId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun attendeesOf(showtimeId: String): Flow<List<TheaterAttendee>> =
        theaterRepository.observeAttendees(showtimeId)
            .map { theaterRepository.resolveAttendees(it) }
}
