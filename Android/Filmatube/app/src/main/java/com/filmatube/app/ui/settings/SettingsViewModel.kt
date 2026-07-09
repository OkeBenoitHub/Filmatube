package com.filmatube.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.filmatube.app.data.download.DownloadRepository
import com.filmatube.app.data.preferences.UserPreferencesRepository
import com.filmatube.app.domain.model.DownloadQuality
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import com.filmatube.app.util.LocaleController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val preferences: UserPreferencesRepository,
    private val downloadRepository: DownloadRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser()?.uid

    private val _language = MutableStateFlow(LocaleController.currentTag())
    val language = _language.asStateFlow()

    val downloadQuality = preferences.downloadQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DownloadQuality.STANDARD)

    val downloadWifiOnly = preferences.downloadWifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val downloadAutoDeleteWatched = preferences.downloadAutoDeleteWatched
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        viewModelScope.launch { downloadRepository.setWifiOnly(preferences.downloadWifiOnly.first()) }
    }

    fun setDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch { preferences.setDownloadQuality(quality) }
    }

    fun setDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setDownloadWifiOnly(enabled)
            downloadRepository.setWifiOnly(enabled)
        }
    }

    fun setDownloadAutoDeleteWatched(enabled: Boolean) {
        viewModelScope.launch { preferences.setDownloadAutoDeleteWatched(enabled) }
    }

    fun setLanguage(tag: String) {
        if (tag == _language.value) return
        _language.value = tag
        uid?.let { id ->
            viewModelScope.launch { runCatching { userRepository.updateLanguage(id, tag) } }
        }
        // Recreates activities to apply the new locale.
        LocaleController.apply(tag)
    }

    fun signOut() = authRepository.signOut()
}
