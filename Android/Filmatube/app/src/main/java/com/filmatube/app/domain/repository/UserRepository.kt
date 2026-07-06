package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.AuthUser

/** Firestore user-profile document operations. */
interface UserRepository {
    /**
     * Creates `users/{uid}` with defaults if it doesn't exist (first sign-in),
     * otherwise refreshes `lastActiveAt`. Idempotent.
     */
    suspend fun ensureUserDocument(user: AuthUser)

    /** True when the user hasn't finished taste onboarding (`tasteCompleted` != true). */
    suspend fun needsTasteOnboarding(uid: String): Boolean

    /** Saves taste preferences and marks onboarding complete. */
    suspend fun saveTaste(
        uid: String,
        genres: List<String>,
        contentLanguage: String,
        language: String,
    )
}
