package com.filmatube.app.ui.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.BoardMember
import com.filmatube.app.data.boards.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembersViewModel @Inject constructor(
    private val boardRepository: BoardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val boardId: String = savedStateHandle["boardId"] ?: ""

    val members = boardRepository.observeMembers(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<BoardMember>())

    private val _ownerId = MutableStateFlow("")
    val ownerId = _ownerId.asStateFlow()

    val myUid: String? get() = boardRepository.myUid

    init {
        viewModelScope.launch { _ownerId.value = boardRepository.getBoard(boardId)?.ownerId ?: "" }
    }

    fun toggleMute(member: BoardMember) {
        viewModelScope.launch { boardRepository.setMuted(boardId, member.uid, !member.muted) }
    }

    fun remove(member: BoardMember) {
        viewModelScope.launch { boardRepository.removeMember(boardId, member.uid) }
    }
}
