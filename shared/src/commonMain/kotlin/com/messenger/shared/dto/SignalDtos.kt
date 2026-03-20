package com.messenger.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class SignalBundleRequest(
    val identityKey: String,
    val signedPreKey: String,
    val oneTimePreKeys: String,
)

@Serializable
data class SignalBundleResponse(
    val identityKey: String,
    val signedPreKey: String,
    val oneTimePreKeys: String,
)
