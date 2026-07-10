package com.filmatube.app.ui.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.social.FollowRepository
import com.filmatube.app.data.social.SocialRepository
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

const val FOLLOW_MODE_FOLLOWERS = "followers"

data class FollowUser(
    val uid: String,
    val displayName: String,
    val avatarUrl: String,
    val isFollowing: Boolean,
    val tasteMatch: Int = 0,
)

@HiltViewModel
class FollowListViewModel @Inject constructor(
    private val followRepository: FollowRepository,
    private val userRepository: UserRepository,
    private val socialRepository: SocialRepository,
    authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** "followers" or "following". */
    val mode: String = savedStateHandle["mode"] ?: FOLLOW_MODE_FOLLOWERS

    private val uid: String? = authRepository.currentUser()?.uid

    val users = run {
        val ids = uid?.let {
            if (mode == FOLLOW_MODE_FOLLOWERS) followRepository.observeFollowerIds(it)
            else followRepository.observeFollowingIds(it)
        } ?: flowOf(emptyList())

        combine(ids, followRepository.observeMyFollowingIds()) { userIds, myFollowing ->
            userIds.mapNotNull { id ->
                userRepository.getUser(id)?.let { profile ->
                    FollowUser(
                        uid = profile.uid,
                        displayName = profile.displayName,
                        avatarUrl = profile.avatarUrl,
                        isFollowing = id in myFollowing,
                        tasteMatch = socialRepository.tasteMatch(profile.genrePreferences),
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    }

    fun toggleFollow(targetUid: String, follow: Boolean) {
        viewModelScope.launch { followRepository.setFollowing(targetUid, follow) }
    }
}
