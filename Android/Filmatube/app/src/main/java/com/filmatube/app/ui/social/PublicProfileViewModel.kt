package com.filmatube.app.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.FollowRepository
import com.filmatube.app.data.social.SocialRepository
import com.filmatube.app.domain.model.UserProfile
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val followRepository: FollowRepository,
    private val socialRepository: SocialRepository,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val userId: String = savedStateHandle["userId"] ?: ""
    val isSelf: Boolean = authRepository.currentUser()?.uid == userId

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile = _profile.asStateFlow()

    private val _tasteMatch = MutableStateFlow(0)
    val tasteMatch = _tasteMatch.asStateFlow()

    val isFollowing = followRepository.isFollowing(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val followerCount = followRepository.observeFollowerIds(userId).map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val followingCount = followRepository.observeFollowingIds(userId).map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            val profile = userRepository.getUser(userId)
            _profile.value = profile
            if (profile != null && !isSelf) {
                _tasteMatch.value = socialRepository.tasteMatch(profile.genrePreferences)
            }
        }
    }

    fun toggleFollow() {
        viewModelScope.launch { followRepository.setFollowing(userId, !isFollowing.value) }
    }
}
