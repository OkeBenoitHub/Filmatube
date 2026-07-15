package com.filmatube.app.ui.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
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

    val isMember = boardRepository.observeMembership(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Number of followers invited on the last invite tap (for a confirmation), or null. */
    private val _invitedCount = MutableStateFlow<Int?>(null)
    val invitedCount = _invitedCount.asStateFlow()

    val isOwner get() = _board.value?.ownerId == boardRepository.myUid

    init {
        viewModelScope.launch { _board.value = boardRepository.getBoard(boardId) }
    }

    fun toggleMembership() {
        viewModelScope.launch {
            if (isMember.value) boardRepository.leaveBoard(boardId) else boardRepository.joinBoard(boardId)
        }
    }

    fun invite() {
        val title = _board.value?.title ?: return
        viewModelScope.launch {
            _invitedCount.value = boardRepository.inviteFollowers(boardId, title)
        }
    }

    fun clearInvited() {
        _invitedCount.value = null
    }
}
