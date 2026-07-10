package com.filmatube.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.notifications.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationCenterViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    val notifications = notificationRepository.observeNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markAllRead() {
        viewModelScope.launch { notificationRepository.markAllRead() }
    }

    fun markRead(id: String) {
        viewModelScope.launch { notificationRepository.markRead(id) }
    }
}
