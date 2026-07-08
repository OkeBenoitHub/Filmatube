package com.filmatube.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.playback.WatchProgressRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.repository.UserRepository
import com.filmatube.app.domain.util.AppError
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenreRow(val genreKey: String, val movies: List<Movie>)

data class ContinueWatchingItem(val movie: Movie, val progress: Float)

data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val featured: List<Movie> = emptyList(),
    val trending: List<Movie> = emptyList(),
    val newReleases: List<Movie> = emptyList(),
    val comingSoon: List<Movie> = emptyList(),
    val genreRows: List<GenreRow> = emptyList(),
    val error: AppError? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null &&
            continueWatching.isEmpty() &&
            featured.isEmpty() && trending.isEmpty() && newReleases.isEmpty() &&
            comingSoon.isEmpty() && genreRows.isEmpty()
}

private val DEFAULT_GENRES = listOf("action", "comedy", "drama", "scifi")

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val watchProgressRepository: WatchProgressRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                val featured = movieRepository.getFeatured()
                val trending = movieRepository.getTrending()
                val newReleases = movieRepository.getNewReleases()
                val comingSoon = movieRepository.getComingSoon()

                val uid = authRepository.currentUser()?.uid
                val userGenres = uid
                    ?.let { userRepository.getUser(it)?.genrePreferences }
                    ?.filter { it.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: DEFAULT_GENRES

                val genreRows = userGenres.take(4)
                    .map { genre -> GenreRow(genre, movieRepository.getByGenre(genre)) }
                    .filter { it.movies.isNotEmpty() }

                val continueWatching = watchProgressRepository.getContinueWatching()
                    .mapNotNull { entry ->
                        movieRepository.getMovie(entry.movieId)?.let { movie ->
                            ContinueWatchingItem(movie, entry.progress)
                        }
                    }
                    .take(12)

                HomeUiState(
                    isLoading = false,
                    continueWatching = continueWatching,
                    featured = featured,
                    trending = trending,
                    newReleases = newReleases,
                    comingSoon = comingSoon,
                    genreRows = genreRows,
                )
            }.fold(
                onSuccess = { loaded -> _state.value = loaded },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.toAppError()) } },
            )
        }
    }
}
