package com.filmatube.app.ui.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardRepository
import com.filmatube.app.data.boards.BoardTypes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Discovery tabs. */
enum class BoardFilter(val type: String?) {
    ALL(null),
    MOVIES(BoardTypes.MOVIE),
    GENERAL(BoardTypes.GENERAL),
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BoardsViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(BoardFilter.ALL)
    val filter = _filter.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    fun setQuery(value: String) {
        _query.value = value
    }

    val featured = boardRepository.observeFeatured()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val myBoards = boardRepository.observeMyBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * The discovery list: filtered by type server-side, then by text here.
     *
     * Firestore has no substring search, so matching title/description/movie happens over the
     * fetched page — the same compromise the web page makes, and the reason both cap the fetch
     * rather than paginating.
     */
    val boards = combine(
        _filter.flatMapLatest { f -> boardRepository.observeBoards(f.type) },
        _query,
    ) { list, q ->
        val needle = q.trim().lowercase()
        if (needle.isEmpty()) {
            list
        } else {
            list.filter { board ->
                listOf(board.title, board.description, board.movieTitle)
                    .any { it.lowercase().contains(needle) }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Board>())

    fun setFilter(f: BoardFilter) {
        _filter.value = f
    }
}
