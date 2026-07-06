package com.filmatube.app.ui.auth

import android.util.Patterns
import androidx.annotation.StringRes
import com.filmatube.app.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

/** Returns a string-res error, or null if valid. */
@StringRes
fun validateEmail(email: String): Int? = when {
    email.isBlank() -> R.string.auth_error_email_required
    !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> R.string.auth_error_email_invalid
    else -> null
}

@StringRes
fun validatePassword(password: String): Int? = when {
    password.isBlank() -> R.string.auth_error_password_required
    password.length < 6 -> R.string.auth_error_password_short
    else -> null
}

/** Maps a Firebase auth failure to a user-facing message. */
@StringRes
fun mapAuthError(throwable: Throwable): Int = when (throwable) {
    is FirebaseAuthInvalidCredentialsException,
    is FirebaseAuthInvalidUserException,
    -> R.string.auth_error_invalid_credentials
    is FirebaseAuthWeakPasswordException -> R.string.auth_error_password_short
    is FirebaseAuthUserCollisionException -> R.string.auth_error_email_in_use
    is FirebaseNetworkException -> R.string.auth_error_network
    is FirebaseTooManyRequestsException -> R.string.auth_error_too_many
    else -> R.string.auth_error_generic
}
