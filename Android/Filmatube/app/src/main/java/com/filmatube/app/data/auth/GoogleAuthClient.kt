package com.filmatube.app.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.filmatube.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Sign-In via the AndroidX Credential Manager. Returns a Google ID token that the
 * [AuthRepository] exchanges for a Firebase credential.
 *
 * Requires `R.string.google_web_client_id` (OAuth 2.0 **Web** client ID) to be set, and the
 * app's SHA-1 registered in Firebase. The credential UI needs an Activity context.
 */
@Singleton
class GoogleAuthClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {
    suspend fun getIdToken(activityContext: Context): String {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(appContext.getString(R.string.google_web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(activityContext)
        val response = credentialManager.getCredential(activityContext, request)
        val credential = response.credential

        if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unexpected credential type: ${credential.type}")
    }
}
