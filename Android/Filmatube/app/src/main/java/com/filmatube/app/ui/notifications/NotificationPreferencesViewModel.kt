package com.filmatube.app.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val preferences: UserPreferencesRepository,
) : ViewModel() {

    val social = preferences.notifSocial.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val content = preferences.notifContent.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val system = preferences.notifSystem.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setSocial(enabled: Boolean) = viewModelScope.launch { preferences.setNotifSocial(enabled) }
    fun setContent(enabled: Boolean) = viewModelScope.launch { preferences.setNotifContent(enabled) }
    fun setSystem(enabled: Boolean) = viewModelScope.launch { preferences.setNotifSystem(enabled) }
}
