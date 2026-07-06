package com.filmatube.app.domain.model

/** Minimal authenticated-user model exposed to the UI/domain (decoupled from FirebaseUser). */
data class AuthUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)
