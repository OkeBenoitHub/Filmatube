package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.AuthUser

/** Firestore user-profile document operations. */
interface UserRepository {
    /**
     * Creates `users/{uid}` with defaults if it doesn't exist (first sign-in),
     * otherwise refreshes `lastActiveAt`. Idempotent.
     */
    suspend fun ensureUserDocument(user: AuthUser)
}
