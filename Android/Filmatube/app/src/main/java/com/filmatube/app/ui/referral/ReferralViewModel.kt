package com.filmatube.app.ui.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.referral.ReferralRepository
import com.filmatube.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReferredFriend(val id: String, val name: String, val avatarUrl: String)

data class ReferralUiState(
    val inviteLink: String = "",
    val friends: List<ReferredFriend> = emptyList(),
)

@HiltViewModel
class ReferralViewModel @Inject constructor(
    private val referralRepository: ReferralRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReferralUiState(inviteLink = referralRepository.inviteLink().orEmpty()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            referralRepository.observeMyReferralIds().collect { ids ->
                val friends = ids.mapNotNull { id ->
                    val user = runCatching { userRepository.getUser(id) }.getOrNull() ?: return@mapNotNull null
                    ReferredFriend(id, user.displayName.ifBlank { "Filmatube friend" }, user.avatarUrl)
                }
                _state.update { it.copy(friends = friends) }
            }
        }
    }
}
