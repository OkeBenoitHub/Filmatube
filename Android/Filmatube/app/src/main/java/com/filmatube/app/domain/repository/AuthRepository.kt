package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/** Authentication operations. Implementations throw on failure; callers map to UI errors. */
interface AuthRepository {
    /** Emits the current user (or null) and updates on sign-in/out. */
    val authState: Flow<AuthUser?>

    fun currentUser(): AuthUser?

    suspend fun signIn(email: String, password: String)

    suspend fun register(name: String, email: String, password: String)

    suspend fun signInWithGoogle(idToken: String)

    suspend fun sendPasswordReset(email: String)

    /** Whether the signed-in user still needs taste onboarding. */
    suspend fun needsTasteOnboarding(): Boolean

    fun signOut()
}
