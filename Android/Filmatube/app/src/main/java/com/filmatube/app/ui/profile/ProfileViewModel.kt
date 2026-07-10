package com.filmatube.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.notifications.NotificationRepository
import com.filmatube.app.data.social.FollowRepository
import com.filmatube.app.domain.model.UserProfile
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import com.filmatube.app.domain.util.DataState
import com.filmatube.app.domain.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    followRepository: FollowRepository,
    notificationRepository: NotificationRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser()?.uid

    val unreadNotifications: StateFlow<Int> = notificationRepository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val state: StateFlow<DataState<UserProfile>> =
        (uid?.let { id ->
            userRepository.observeUser(id).map { profile ->
                if (profile == null) DataState.Empty else DataState.Success(profile)
            }
        } ?: flowOf(DataState.Empty))
            .catch { emit(DataState.Error(it.toAppError())) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DataState.Loading,
            )

    val followerCount: StateFlow<Int> =
        (uid?.let { followRepository.observeFollowerIds(it).map { ids -> ids.size } } ?: flowOf(0))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val followingCount: StateFlow<Int> =
        (uid?.let { followRepository.observeFollowingIds(it).map { ids -> ids.size } } ?: flowOf(0))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
