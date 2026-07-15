package com.filmatube.app.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.filmatube.app.data.download.DownloadRepository
import com.filmatube.app.data.library.WatchlistRepository
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardRepository
import com.filmatube.app.data.social.FeedEventTypes
import com.filmatube.app.data.social.FeedRepository
import com.filmatube.app.data.social.RatingAggregate
import com.filmatube.app.data.social.RatingRepository
import com.filmatube.app.data.social.ReactionRepository
import com.filmatube.app.data.social.RecipientUser
import com.filmatube.app.data.social.RecommendationRepository
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

/** Simplified download state for the detail screen's download button. */
enum class DownloadUiState { NONE, DOWNLOADING, DOWNLOADED }

@UnstableApi
@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val preferences: UserPreferencesRepository,
    private val downloadRepository: DownloadRepository,
    private val watchlistRepository: WatchlistRepository,
    private val feedRepository: FeedRepository,
    private val reactionRepository: ReactionRepository,
    private val ratingRepository: RatingRepository,
    private val recommendationRepository: RecommendationRepository,
    private val boardRepository: BoardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val movieId: String = savedStateHandle["movieId"] ?: ""

    private val _state = MutableStateFlow(DetailUiState())
    val state = _state.asStateFlow()

    val myReaction = reactionRepository.observeMyReaction(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _reactionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val reactionCounts = _reactionCounts.asStateFlow()

    fun setReaction(type: String) {
        val movie = (state.value.movie as? DataState.Success)?.data
        val newType = if (myReaction.value == type) null else type
        viewModelScope.launch {
            reactionRepository.setReaction(movieId, newType)
            _reactionCounts.value = reactionRepository.reactionCounts(movieId)
            if (newType != null && movie != null) {
                feedRepository.publish(FeedEventTypes.REACTED, movieId, movie.title.get("en"))
            }
        }
    }

    private fun loadReactionCounts() {
        viewModelScope.launch { _reactionCounts.value = reactionRepository.reactionCounts(movieId) }
    }

    val myRating = ratingRepository.observeMyRating(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ratingAggregate = ratingRepository.observeAggregate(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RatingAggregate(0.0, 0))

    fun setRating(value: Int) {
        val current = myRating.value
        viewModelScope.launch {
            ratingRepository.setRating(movieId, if (current == value) null else value)
        }
    }

    private val _recipients = MutableStateFlow<List<RecipientUser>>(emptyList())
    val recipients = _recipients.asStateFlow()

    fun loadRecipients() {
        viewModelScope.launch { _recipients.value = recommendationRepository.recipients() }
    }

    /** Boards the user can share this movie into (owned or joined). */
    val myBoards = boardRepository.observeMyBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Board>())

    private val _sharedToBoard = MutableStateFlow<String?>(null)
    val sharedToBoard = _sharedToBoard.asStateFlow()

    fun shareToBoard(board: Board) {
        val movie = (state.value.movie as? DataState.Success)?.data ?: return
        viewModelScope.launch {
            boardRepository.postMovieCard(board.id, movie.id, movie.title.get("en"), movie.posterUrl)
            _sharedToBoard.value = board.title
        }
    }

    fun clearSharedToBoard() {
        _sharedToBoard.value = null
    }

    fun recommend(toUid: String, message: String) {
        val movie = (state.value.movie as? DataState.Success)?.data
        viewModelScope.launch {
            recommendationRepository.send(toUid, movieId, movie?.title?.get("en") ?: "", message.trim())
        }
    }

    val reminderSet = preferences.reminders
        .map { movieId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val savedForLater = watchlistRepository.observeSavedIds()
        .map { movieId in it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun toggleSaved() {
        val movie = (state.value.movie as? DataState.Success)?.data
        val wasSaved = savedForLater.value
        viewModelScope.launch {
            watchlistRepository.toggle(movieId)
            if (!wasSaved && movie != null) {
                feedRepository.publish(FeedEventTypes.ADDED_WATCHLIST, movieId, movie.title.get("en"))
            }
        }
    }

    val downloadState = downloadRepository.items()
        .map { items ->
            val item = items.firstOrNull { it.movieId == movieId }
            when {
                item == null -> DownloadUiState.NONE
                item.isComplete -> DownloadUiState.DOWNLOADED
                else -> DownloadUiState.DOWNLOADING
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadUiState.NONE)

    fun toggleReminder() {
        viewModelScope.launch { preferences.toggleReminder(movieId) }
    }

    fun toggleDownload() {
        val movie = (state.value.movie as? DataState.Success)?.data ?: return
        viewModelScope.launch {
            if (downloadState.value == DownloadUiState.NONE) {
                runCatching { downloadRepository.download(movie) }
            } else {
                downloadRepository.cancel(movie.id)
            }
        }
    }

    init {
        load()
        loadReactionCounts()
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
