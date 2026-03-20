package com.messenger.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val url: String,
    val contentType: String,
    val size: Long,
)
