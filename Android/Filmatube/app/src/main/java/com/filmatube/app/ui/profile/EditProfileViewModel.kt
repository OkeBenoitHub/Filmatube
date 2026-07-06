package com.filmatube.app.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.R
import com.filmatube.app.data.upload.AvatarUploader
import com.filmatube.app.domain.repository.AuthRepository
import com.filmatube.app.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val displayName: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val pickedAvatar: Uri? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val nameError: Boolean = false,
    val errorMessage: Int? = null,
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val avatarUploader: AvatarUploader,
) : ViewModel() {

    private val uid: String? = authRepository.currentUser()?.uid
    private val _state = MutableStateFlow(EditProfileUiState())
    val state = _state.asStateFlow()

    init {
        val currentUid = uid
        if (currentUid == null) {
            _state.update { it.copy(isLoading = false) }
        } else {
            viewModelScope.launch {
                val profile = runCatching { userRepository.getUser(currentUid) }.getOrNull()
                _state.update {
                    it.copy(
                        displayName = profile?.displayName.orEmpty(),
                        bio = profile?.bio.orEmpty(),
                        avatarUrl = profile?.avatarUrl.orEmpty(),
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(displayName = value, nameError = false) }
    fun onBioChange(value: String) = _state.update { it.copy(bio = value) }
    fun onAvatarPicked(uri: Uri) = _state.update { it.copy(pickedAvatar = uri) }

    fun save() {
        val current = _state.value
        val currentUid = uid ?: return
        if (current.displayName.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        if (current.isSaving) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val newAvatarUrl = current.pickedAvatar?.let { avatarUploader.uploadAvatar(it) }
                userRepository.updateProfile(currentUid, current.displayName.trim(), current.bio.trim())
                if (newAvatarUrl != null) {
                    userRepository.updateAvatarUrl(currentUid, newAvatarUrl)
                }
                _state.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: Exception) {
                val message = if (current.pickedAvatar != null) {
                    R.string.edit_profile_photo_error
                } else {
                    R.string.edit_profile_error
                }
                _state.update { it.copy(isSaving = false, errorMessage = message) }
            }
        }
    }
}
