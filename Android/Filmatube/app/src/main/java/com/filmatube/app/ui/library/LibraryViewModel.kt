package com.filmatube.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.library.WatchlistRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    watchlistRepository: WatchlistRepository,
    movieRepository: MovieRepository,
) : ViewModel() {

    val movies = watchlistRepository.observeSavedIds()
        .map { ids -> ids.mapNotNull { runCatching { movieRepository.getMovie(it) }.getOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Movie>())
}
