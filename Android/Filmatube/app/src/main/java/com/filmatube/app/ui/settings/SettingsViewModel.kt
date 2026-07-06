package com.filmatube.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import com.filmatube.app.util.LocaleController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser()?.uid

    private val _language = MutableStateFlow(LocaleController.currentTag())
    val language = _language.asStateFlow()

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
