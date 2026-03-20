package com.messenger.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class CallOffer(
    val chatId: String,
    val callType: String,
    val sdp: String? = null,
)

@Serializable
data class CallAnswer(
    val callId: String,
    val sdp: String,
)

@Serializable
data class IceCandidate(
    val callId: String,
    val candidate: String,
)
