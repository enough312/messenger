package com.messenger.shared.dto

import com.messenger.shared.model.MessageType
import kotlinx.serialization.Serializable

@Serializable
data class SendMessageRequest(
    val content: String? = null,
    val type: MessageType = MessageType.TEXT,
    val mediaUrl: String? = null,
    val mediaSize: Long? = null,
    val mediaMime: String? = null,
    val thumbUrl: String? = null,
    val replyToId: String? = null,
)

@Serializable
data class UpdateMessageRequest(
    val content: String,
)

@Serializable
data class ReactionRequest(
    val emoji: String,
)

@Serializable
data class ReadMessageRequest(
    val chatId: String,
    val messageId: String,
)
