package com.messenger.shared.dto

import com.messenger.shared.model.User
import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null,
)

@Serializable
data class UserSearchResponse(
    val items: List<User>,
)
