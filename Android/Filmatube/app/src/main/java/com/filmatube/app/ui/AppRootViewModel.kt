package com.filmatube.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.notifications.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Registers this device's FCM token once the signed-in app shell is mounted. */
@HiltViewModel
class AppRootViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {
    fun registerPushToken() {
        viewModelScope.launch { notificationRepository.registerToken() }
    }
}
