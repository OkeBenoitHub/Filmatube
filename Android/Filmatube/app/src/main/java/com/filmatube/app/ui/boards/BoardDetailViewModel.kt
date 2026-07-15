package com.filmatube.app.ui.boards

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.boards.BoardMessage
import com.filmatube.app.data.boards.BoardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    val board = boardRepository.observeBoard(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isMember = boardRepository.observeMembership(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val amMuted = boardRepository.observeMyMuted(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val messages = boardRepository.observeMessages(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<BoardMessage>())

    val typingNames = boardRepository.observeTyping(boardId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<String>())

    private val _draft = MutableStateFlow("")
    val draft = _draft.asStateFlow()

    private val _spoiler = MutableStateFlow(false)
    val spoiler = _spoiler.asStateFlow()

    private val _replyingTo = MutableStateFlow<BoardMessage?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    private var typingJob: Job? = null

    fun setReplyTo(message: BoardMessage?) {
        _replyingTo.value = message
    }

    fun toggleReaction(message: BoardMessage, emoji: String) {
        val uid = boardRepository.myUid ?: return
        viewModelScope.launch {
            boardRepository.toggleReaction(boardId, message.id, emoji, message.reactions[uid])
        }
    }

    fun setDraft(v: String) {
        _draft.value = v
        // Debounced typing: set typing now, auto-clear after 4s of no change.
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            boardRepository.setTyping(boardId, v.isNotBlank())
            if (v.isNotBlank()) {
                delay(4_000)
                boardRepository.setTyping(boardId, false)
            }
        }
    }

    fun setSpoiler(v: Boolean) {
        _spoiler.value = v
    }

    fun send() {
        val text = _draft.value.trim()
        if (text.isBlank()) return
        val spoiler = _spoiler.value
        val replyTo = _replyingTo.value
        _draft.value = ""
        _spoiler.value = false
        _replyingTo.value = null
        typingJob?.cancel()
        viewModelScope.launch { boardRepository.sendMessage(boardId, text, spoiler, replyTo) }
    }

    fun deleteMessage(message: BoardMessage) {
        viewModelScope.launch { boardRepository.deleteMessage(boardId, message.id) }
    }

    /** Number of followers invited on the last invite tap (for a confirmation), or null. */
    private val _invitedCount = MutableStateFlow<Int?>(null)
    val invitedCount = _invitedCount.asStateFlow()

    val isOwner get() = board.value?.ownerId == boardRepository.myUid
    val myUid: String? get() = boardRepository.myUid

    fun toggleMembership() {
        viewModelScope.launch {
            if (isMember.value) boardRepository.leaveBoard(boardId) else boardRepository.joinBoard(boardId)
        }
    }

    fun invite() {
        val title = board.value?.title ?: return
        viewModelScope.launch {
            _invitedCount.value = boardRepository.inviteFollowers(boardId, title)
        }
    }

    fun clearInvited() {
        _invitedCount.value = null
    }

    fun pinMessage(message: BoardMessage) {
        val current = board.value?.pinnedMessageId
        viewModelScope.launch {
            boardRepository.pinMessage(boardId, if (current == message.id) "" else message.id)
        }
    }

    fun reportMessage(message: BoardMessage) {
        viewModelScope.launch { boardRepository.reportMessage(boardId, message.id, message.userId) }
    }
}
