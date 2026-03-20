package com.messenger.model

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val message: String,
)
