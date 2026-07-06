package com.filmatube.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val movie: DataState<Movie> = DataState.Loading,
    val related: List<Movie> = emptyList(),
)

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val preferences: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = savedStateHandle["movieId"] ?: ""

    private val _state = MutableStateFlow(DetailUiState())
    val state = _state.asStateFlow()

    val reminderSet = preferences.reminders
        .map { movieId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleReminder() {
        viewModelScope.launch { preferences.toggleReminder(movieId) }
    }

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(movie = DataState.Loading) }
            runCatching { movieRepository.getMovie(movieId) }.fold(
                onSuccess = { movie ->
                    if (movie == null) {
                        _state.update { it.copy(movie = DataState.Empty) }
                    } else {
                        _state.update { it.copy(movie = DataState.Success(movie)) }
                        loadRelated(movie)
                    }
                },
                onFailure = { error -> _state.update { it.copy(movie = DataState.Error(error.toAppError())) } },
            )
        }
    }

    private fun loadRelated(movie: Movie) {
        viewModelScope.launch {
            val related = runCatching {
                movieRepository.getRelated(movie.id, movie.genres)
            }.getOrDefault(emptyList())
            _state.update { it.copy(related = related) }
        }
    }
}
