package com.filmatube.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.playback.WatchProgressRepository
import com.filmatube.app.data.recs.RecsRepository
import com.filmatube.app.data.social.FeedRepository
import com.filmatube.app.domain.model.Movie
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.MovieRepository
import com.filmatube.app.domain.repository.MovieSort
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

/** A resolved "Because you watched X" rail — the seed's title, and the recommended movies. */
data class BecauseYouWatchedRow(val seedTitle: String, val movies: List<Movie>)

data class ContinueWatchingItem(val movie: Movie, val progress: Float)

data class HomeUiState(
    val isLoading: Boolean = true,
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
    val featured: List<Movie> = emptyList(),
    val trending: List<Movie> = emptyList(),
    val newReleases: List<Movie> = emptyList(),
    val comingSoon: List<Movie> = emptyList(),
    val genreRows: List<GenreRow> = emptyList(),
    val becauseYouWatched: List<BecauseYouWatchedRow> = emptyList(),
    val topPicks: List<Movie> = emptyList(),
    val fromPeopleYouFollow: List<Movie> = emptyList(),
    val hiddenGems: List<Movie> = emptyList(),
    val newForYou: List<Movie> = emptyList(),
    val error: AppError? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && error == null &&
            continueWatching.isEmpty() &&
            featured.isEmpty() && trending.isEmpty() && newReleases.isEmpty() &&
            comingSoon.isEmpty() && genreRows.isEmpty()
}

private val DEFAULT_GENRES = listOf("action", "comedy", "drama", "scifi")

private const val ROW_LIMIT = 15
private const val GEM_MIN_RATING = 3.8
private const val GEM_MAX_VIEWS = 500L

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val watchProgressRepository: WatchProgressRepository,
    private val recsRepository: RecsRepository,
    private val feedRepository: FeedRepository,
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

                // Personalised rails from the nightly rec doc. Empty (and silently absent)
                // until the function has built recs for this user — never a blocking error.
                val recs = recsRepository.getRecommendations()
                val becauseYouWatched = recs.rows
                    .map { row -> row to movieRepository.getMoviesByIds(row.movieIds) }
                    .filter { (_, movies) -> movies.size >= 3 }
                    .map { (row, movies) -> BecauseYouWatchedRow(row.seedTitle, movies) }
                val topPicks = movieRepository.getMoviesByIds(recs.topPicks)

                // What the accounts this user follows have been watching, minus anything they've
                // already finished themselves. Absent for an account that follows nobody.
                val finishedIds = watchProgressRepository.getWatchedIds()
                val fromPeopleYouFollow = movieRepository
                    .getMoviesByIds(feedRepository.getTrendingAmongFollowing().take(ROW_LIMIT * 2))
                    .filter { it.id !in finishedIds }
                    .take(ROW_LIMIT)

                // Hidden gems: well-reviewed but under-watched, from a rating-sorted pool. The
                // view cap is what makes them "hidden" — otherwise this is just Top Rated.
                val hiddenGems = movieRepository
                    .browse(sort = MovieSort.RATING, comingSoon = false, limit = 60)
                    .filter { it.averageRating >= GEM_MIN_RATING && it.viewsCount <= GEM_MAX_VIEWS }
                    .take(ROW_LIMIT)

                // New for you: this week's arrivals narrowed to the genres the user actually
                // picked — the personalised half of New Releases.
                val newForYou = newReleases
                    .filter { movie -> movie.genres.any { it in userGenres } }
                    .take(ROW_LIMIT)

                HomeUiState(
                    isLoading = false,
                    continueWatching = continueWatching,
                    featured = featured,
                    trending = trending,
                    newReleases = newReleases,
                    comingSoon = comingSoon,
                    genreRows = genreRows,
                    becauseYouWatched = becauseYouWatched,
                    topPicks = topPicks,
                    fromPeopleYouFollow = fromPeopleYouFollow,
                    hiddenGems = hiddenGems,
                    newForYou = newForYou,
                )
            }.fold(
                onSuccess = { loaded -> _state.value = loaded },
                onFailure = { e -> _state.update { it.copy(isLoading = false, error = e.toAppError()) } },
            )
        }
    }

    /**
     * "Not interested" on a recommended title.
     *
     * Drops it from every rec rail on screen straight away rather than waiting for a reload —
     * a dismissal that leaves the poster sitting there reads as a no-op. The write is what
     * makes it stick: the next nightly build reads recFeedback and excludes it. Rails that fall
     * below three movies as a result disappear, matching how they were built.
     */
    fun notInterested(movieId: String) {
        _state.update { current ->
            current.copy(
                topPicks = current.topPicks.filterNot { it.id == movieId },
                becauseYouWatched = current.becauseYouWatched
                    .map { row -> row.copy(movies = row.movies.filterNot { it.id == movieId }) }
                    .filter { it.movies.size >= 3 },
            )
        }
        viewModelScope.launch { recsRepository.dismiss(movieId) }
    }
}
