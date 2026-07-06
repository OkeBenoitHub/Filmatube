package com.filmatube.app.domain.model

/** The `users/{uid}` Firestore document as consumed by the UI. */
data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String,
    val bio: String,
    val avatarUrl: String,
    val language: String,
    val followersCount: Long,
    val followingCount: Long,
    val isAdmin: Boolean,
    val genrePreferences: List<String> = emptyList(),
)
