package com.filmatube.app.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.R
import com.filmatube.app.data.auth.GoogleAuthClient
import com.filmatube.app.data.referral.ReferralRepository
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
    val isGoogleLoading: Boolean = false,
    val navTarget: AuthNavTarget? = null,
) {
    /** Any auth attempt in flight — used to disable inputs regardless of which one is running. */
    val isBusy: Boolean get() = isLoading || isGoogleLoading
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val referralRepository: ReferralRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state = _state.asStateFlow()

    fun onNameChange(v: String) = _state.update { it.copy(name = v, nameError = null, generalError = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, emailError = null, generalError = null) }
    // Editing the password also clears the mismatch error: it was raised about the *pair*,
    // so leaving it pinned under the confirm field claims a mismatch that may no longer hold.
    fun onPasswordChange(v: String) =
        _state.update { it.copy(password = v, passwordError = null, confirmError = null, generalError = null) }
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
            // Tracked separately from isLoading so the spinner appears on the Google button
            // rather than on the untouched "Create account" button.
            _state.update { it.copy(isGoogleLoading = true, generalError = null) }
            runCatching {
                val idToken = googleAuthClient.getIdToken(activityContext)
                authRepository.signInWithGoogle(idToken)
            }.fold(
                onSuccess = { _state.update { it.copy(isGoogleLoading = false, navTarget = resolveTarget()) } },
                onFailure = { e ->
                    if (e is GetCredentialCancellationException) {
                        _state.update { it.copy(isGoogleLoading = false) }
                    } else {
                        _state.update { it.copy(isGoogleLoading = false, generalError = mapAuthError(e)) }
                    }
                },
            )
        }
    }

    private suspend fun resolveTarget(): AuthNavTarget {
        val needsTaste = runCatching { authRepository.needsTasteOnboarding() }.getOrDefault(true)
        // A fresh account still needs taste onboarding — the moment to attribute a captured invite.
        // The server ignores it for existing accounts, so this is safe even on Google sign-in of a
        // returning user via the register screen.
        if (needsTaste) referralRepository.attributePendingInvite()
        return if (needsTaste) AuthNavTarget.TASTE else AuthNavTarget.MAIN
    }
}
