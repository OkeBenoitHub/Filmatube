package com.filmatube.app.data.upload

import kotlinx.serialization.Serializable

@Serializable
data class PresignRequest(
    val bucket: String,
    val filename: String,
    val contentType: String,
)

@Serializable
data class PresignResponse(
    val uploadUrl: String,
    val key: String,
    val bucket: String,
    val publicUrl: String? = null,
)
