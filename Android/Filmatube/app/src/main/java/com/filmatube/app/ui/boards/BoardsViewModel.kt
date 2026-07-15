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

    val featured = boardRepository.observeFeatured()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val boards = _filter
        .flatMapLatest { f -> boardRepository.observeBoards(f.type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Board>())

    fun setFilter(f: BoardFilter) {
        _filter.value = f
    }
}
