package com.filmatube.app.ui.taste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.R
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import com.filmatube.app.util.LocaleController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasteUiState(
    val selectedGenres: Set<String> = emptySet(),
    val appLanguage: String = "en",
    val contentLanguage: String = "both",
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val errorRes: Int? = null,
) {
    val canContinue: Boolean get() = selectedGenres.isNotEmpty()
}

@HiltViewModel
class TasteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TasteUiState(appLanguage = LocaleController.currentTag()))
    val state = _state.asStateFlow()

    fun toggleGenre(key: String) = _state.update { current ->
        val genres = current.selectedGenres.toMutableSet().apply {
            if (!add(key)) remove(key)
        }
        current.copy(selectedGenres = genres)
    }

    fun setAppLanguage(tag: String) = _state.update { it.copy(appLanguage = tag) }

    fun setContentLanguage(value: String) = _state.update { it.copy(contentLanguage = value) }

    fun save() {
        val current = _state.value
        if (!current.canContinue || current.isSaving) return
        val uid = authRepository.currentUser()?.uid ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorRes = null) }
            runCatching {
                userRepository.saveTaste(
                    uid = uid,
                    genres = current.selectedGenres.toList(),
                    contentLanguage = current.contentLanguage,
                    language = current.appLanguage,
                )
            }.onSuccess {
                // Mark saved first so the finish navigation is queued, THEN apply the UI
                // language — a language change recreates the activity, which would otherwise
                // race the navigation (on recreation the splash routes to main anyway).
                _state.update { it.copy(isSaving = false, isSaved = true) }
                LocaleController.apply(current.appLanguage)
            }.onFailure {
                _state.update { it.copy(isSaving = false, errorRes = R.string.taste_save_error) }
            }
        }
    }
}
