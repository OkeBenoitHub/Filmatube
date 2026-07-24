package com.filmatube.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.collections.CollectionsRepository
import com.filmatube.app.data.library.WatchlistRepository
import com.filmatube.app.data.playback.WatchProgressRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.model.MovieCollection
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.ui.home.ContinueWatchingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    watchlistRepository: WatchlistRepository,
    collectionsRepository: CollectionsRepository,
    private val movieRepository: MovieRepository,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    /** Watch Later — live, so removing a title elsewhere drops it here without a reload. */
    val watchlist = watchlistRepository.observeSavedIds()
        .map { ids -> ids.mapNotNull { runCatching { movieRepository.getMovie(it) }.getOrNull() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Movie>())

    /** Collections — live from `collections/{id}`, created on web, viewable here. */
    val collections = collectionsRepository.observeMyCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<MovieCollection>())

    private val _continueWatching = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatching = _continueWatching.asStateFlow()

    init {
        viewModelScope.launch {
            _continueWatching.value = watchProgressRepository.getContinueWatching()
                .mapNotNull { entry ->
                    movieRepository.getMovie(entry.movieId)?.let { ContinueWatchingItem(it, entry.progress) }
                }
                .take(12)
        }
    }
}
