package com.filmatube.app.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.R
import com.filmatube.app.data.auth.GoogleAuthClient
import com.filmatube.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: Int? = null,
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val confirmError: Int? = null,
    val generalError: Int? = null,
    val isLoading: Boolean = false,
    val navTarget: AuthNavTarget? = null,
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null, generalError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, generalError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, passwordError = null, generalError = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirmPassword = v, confirmError = null, generalError = null) }

    fun register() {
        val s = _state.value
        val nameError = if (s.name.isBlank()) R.string.auth_error_name_required else null
        val emailError = validateEmail(s.email)
        val passwordError = validatePassword(s.password)
        val confirmError = if (s.password != s.confirmPassword) R.string.auth_error_password_mismatch else null

        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _state.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmError = confirmError,
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            runCatching { authRepository.register(s.name.trim(), s.email.trim(), s.password) }
                .fold(
                    onSuccess = { _state.update { it.copy(isLoading = false, navTarget = resolveTarget()) } },
                    onFailure = { e -> _state.update { it.copy(isLoading = false, generalError = mapAuthError(e)) } },
                )
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            runCatching {
                val idToken = googleAuthClient.getIdToken(activityContext)
                authRepository.signInWithGoogle(idToken)
            }.fold(
                onSuccess = { _state.update { it.copy(isLoading = false, navTarget = resolveTarget()) } },
                onFailure = { e ->
                    if (e is GetCredentialCancellationException) {
                        _state.update { it.copy(isLoading = false) }
                    } else {
                        _state.update { it.copy(isLoading = false, generalError = mapAuthError(e)) }
                    }
                },
            )
        }
    }

    private suspend fun resolveTarget(): AuthNavTarget {
        val needsTaste = runCatching { authRepository.needsTasteOnboarding() }.getOrDefault(true)
        return if (needsTaste) AuthNavTarget.TASTE else AuthNavTarget.MAIN
    }
}
