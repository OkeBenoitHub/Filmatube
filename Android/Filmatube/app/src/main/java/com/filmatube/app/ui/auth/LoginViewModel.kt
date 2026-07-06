package com.filmatube.app.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.data.auth.GoogleAuthClient
import com.filmatube.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: Int? = null,
    val passwordError: Int? = null,
    val generalError: Int? = null,
    val isLoading: Boolean = false,
    val navTarget: AuthNavTarget? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, generalError = null) }

    fun onPasswordChange(value: String) =
        _state.update { it.copy(password = value, passwordError = null, generalError = null) }

    fun signIn() {
        val current = _state.value
        val emailError = validateEmail(current.email)
        val passwordError = if (current.password.isBlank()) {
            com.filmatube.app.R.string.auth_error_password_required
        } else {
            null
        }
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            runCatching { authRepository.signIn(current.email.trim(), current.password) }
                .fold(
                    onSuccess = { _state.update { it.copy(isLoading = false, navTarget = resolveTarget()) } },
                    onFailure = { e ->
                        _state.update { it.copy(isLoading = false, generalError = mapAuthError(e)) }
                    },
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
                        _state.update { it.copy(isLoading = false) } // user dismissed — no error
                    } else {
                        _state.update { it.copy(isLoading = false, generalError = mapAuthError(e)) }
                    }
                },
            )
        }
    }

    private suspend fun resolveTarget(): AuthNavTarget {
        val needsTaste = runCatching { authRepository.needsTasteOnboarding() }.getOrDefault(false)
        return if (needsTaste) AuthNavTarget.TASTE else AuthNavTarget.MAIN
    }
}
