package com.filmatube.app.ui.parties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.parties.PartyRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Scheduling choices offered when creating a party (offset from "now"). */
enum class PartyStart(val offsetMs: Long) {
    NOW(0L),
    IN_30_MIN(30L * 60 * 1000),
    IN_1_HOUR(60L * 60 * 1000),
    IN_2_HOURS(2L * 60 * 60 * 1000),
}

data class CreatePartyUiState(
    val movie: Movie? = null,
    val start: PartyStart = PartyStart.NOW,
    val isSaving: Boolean = false,
    val createdId: String? = null,
    val error: Boolean = false,
) {
    val canCreate: Boolean get() = movie != null && !isSaving
}

@HiltViewModel
class CreatePartyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val movieRepository: MovieRepository,
    private val partyRepository: PartyRepository,
) : ViewModel() {

    private val movieId: String = checkNotNull(savedStateHandle["movieId"])

    private val _state = MutableStateFlow(CreatePartyUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val movie = runCatching { movieRepository.getMovie(movieId) }.getOrNull()
            _state.update { it.copy(movie = movie) }
        }
    }

    fun setStart(start: PartyStart) = _state.update { it.copy(start = start) }

    fun create(language: String) {
        val s = _state.value
        val movie = s.movie ?: return
        if (!s.canCreate) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = false) }
            val id = partyRepository.createParty(
                movieId = movie.id,
                movieTitle = movie.title.get(language),
                moviePoster = movie.posterUrl,
                scheduledAtMs = System.currentTimeMillis() + s.start.offsetMs,
            )
            if (id != null) {
                _state.update { it.copy(isSaving = false, createdId = id) }
            } else {
                _state.update { it.copy(isSaving = false, error = true) }
            }
        }
    }
}
