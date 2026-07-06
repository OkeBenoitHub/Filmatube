package com.filmatube.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filmatube.app.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: Int? = null,
    val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isSent: Boolean = false,
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ForgotPasswordUiState())
    val state = _state.asStateFlow()

    fun onEmailChange(value: String) =
        _state.update { it.copy(email = value, emailError = null, generalError = null) }

    fun sendReset() {
        val emailError = validateEmail(_state.value.email)
        if (emailError != null) {
            _state.update { it.copy(emailError = emailError) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, generalError = null) }
            runCatching { authRepository.sendPasswordReset(_state.value.email.trim()) }
                .fold(
                    onSuccess = { _state.update { it.copy(isLoading = false, isSent = true) } },
                    onFailure = { e ->
                        // Don't reveal whether an account exists — treat "no user" as sent.
                        if (e is FirebaseAuthInvalidUserException) {
                            _state.update { it.copy(isLoading = false, isSent = true) }
                        } else {
                            _state.update { it.copy(isLoading = false, generalError = mapAuthError(e)) }
                        }
                    },
                )
        }
    }
}
