package com.filmatube.app.ui.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BoardDetailViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val boardId: String = savedStateHandle["boardId"] ?: ""

    private val _board = MutableStateFlow<Board?>(null)
    val board = _board.asStateFlow()

    init {
        viewModelScope.launch { _board.value = boardRepository.getBoard(boardId) }
    }
}
