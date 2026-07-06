package com.filmatube.app.domain.model

/** A Netflix-style watch profile under `users/{uid}/profiles/{id}`. */
data class WatchProfile(
    val id: String,
    val name: String,
    val avatarEmoji: String,
    val isDefault: Boolean,
    val language: String,
)
