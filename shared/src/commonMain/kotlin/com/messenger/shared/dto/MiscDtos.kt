package com.messenger.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class ContactRequest(
    val userId: String,
)
