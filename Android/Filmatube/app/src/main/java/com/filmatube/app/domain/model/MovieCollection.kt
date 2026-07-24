package com.filmatube.app.domain.model

/** A user-curated collection (from `collections/{id}`). Created/edited on both platforms now. */
data class MovieCollection(
    val id: String,
    val title: String,
    val coverUrl: String,
    val isPublic: Boolean,
    val userId: String = "",
)
