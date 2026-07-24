package com.filmatube.app.domain.model

/** A user-curated collection (from `collections/{id}`). Read-only on Android; created on web. */
data class MovieCollection(
    val id: String,
    val title: String,
    val coverUrl: String,
    val isPublic: Boolean,
)
