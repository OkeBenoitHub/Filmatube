package com.filmatube.app.ui.parties

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.boards.Board
import com.filmatube.app.data.parties.Party
import com.filmatube.app.data.parties.PartyMember
import com.filmatube.app.data.parties.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result banner after sending invites. */
data class InviteResult(val count: Int)

@HiltViewModel
class PartyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val partyRepository: PartyRepository,
) : ViewModel() {

    private val partyId: String = checkNotNull(savedStateHandle["partyId"])

    val party = partyRepository.observeParty(partyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isMember = partyRepository.observeMembership(partyId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isHost = party
        .map { it != null && it.hostId == partyRepository.myUid }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Members with names/avatars resolved from users/{uid} (member docs hold only uid/role). */
    private val _members = MutableStateFlow<List<PartyMember>>(emptyList())
    val members = _members.asStateFlow()

    val myBoards = partyRepository.observeMyBoards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList<Board>())

    private val _inviteResult = MutableStateFlow<InviteResult?>(null)
    val inviteResult = _inviteResult.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    init {
        viewModelScope.launch {
            partyRepository.observeMembers(partyId).collect { raw ->
                _members.value = raw.map { partyRepository.resolveMember(it) }
            }
        }
    }

    fun join() = launchBusy { partyRepository.joinParty(partyId) }
    fun leave() = launchBusy { partyRepository.leaveParty(partyId) }
    fun start() = launchBusy { partyRepository.startParty(partyId) }
    fun end() = launchBusy { partyRepository.endParty(partyId) }

    /** Host hands the room to a guest and steps down (so the party survives them leaving). */
    fun makeHost(uid: String) = launchBusy { partyRepository.transferHost(partyId, uid) }

    fun inviteFollowers() {
        val p = party.value ?: return
        launchBusy { _inviteResult.value = InviteResult(partyRepository.inviteFollowers(p.id, p)) }
    }

    fun inviteBoard(board: Board) {
        val p = party.value ?: return
        launchBusy { _inviteResult.value = InviteResult(partyRepository.inviteBoardMembers(p.id, p, board.id)) }
    }

    fun dismissInviteResult() {
        _inviteResult.value = null
    }

    private fun launchBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.update { true }
            block()
            _busy.update { false }
        }
    }
}
