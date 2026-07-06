package com.filmatube.app.domain.repository

import com.filmatube.app.domain.model.AuthUser
import com.filmatube.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** Firestore user-profile document operations. */
interface UserRepository {

    /** Real-time stream of the `users/{uid}` document (null if it doesn't exist). */
    fun observeUser(uid: String): Flow<UserProfile?>
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

    /** One-shot read of the user document. */
    suspend fun getUser(uid: String): UserProfile?

    /** Updates editable profile fields. */
    suspend fun updateProfile(uid: String, displayName: String, bio: String)

    /** Updates the avatar URL (after uploading to R2). */
    suspend fun updateAvatarUrl(uid: String, avatarUrl: String)
}
