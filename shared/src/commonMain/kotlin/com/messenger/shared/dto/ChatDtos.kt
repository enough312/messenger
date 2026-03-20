package com.messenger.shared.dto

import com.messenger.shared.model.Chat
import com.messenger.shared.model.ChatRole
import com.messenger.shared.model.ChatType
import com.messenger.shared.model.Message
import kotlinx.serialization.Serializable

@Serializable
data class CreatePrivateChatRequest(
    val peerUserId: String,
)

@Serializable
data class CreateGroupChatRequest(
    val name: String,
    val memberIds: List<String>,
    val description: String? = null,
    val isPublic: Boolean = false,
)

@Serializable
data class CreateChannelRequest(
    val name: String,
    val description: String? = null,
    val isPublic: Boolean = true,
)

@Serializable
data class UpdateChatRequest(
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
    val isPublic: Boolean? = null,
)

@Serializable
data class AddChatMemberRequest(
    val userId: String,
    val role: ChatRole = ChatRole.MEMBER,
)

@Serializable
data class UpdateChatMemberRoleRequest(
    val role: ChatRole,
)

@Serializable
data class ChatListResponse(
    val items: List<Chat>,
)

@Serializable
data class MessageListResponse(
    val items: List<Message>,
    val nextCursor: String? = null,
)

@Serializable
data class ChatSummary(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val lastMessage: Message? = null,
)
