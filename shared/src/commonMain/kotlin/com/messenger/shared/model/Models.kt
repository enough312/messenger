package com.messenger.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class UserStatus {
    ONLINE,
    OFFLINE,
    AWAY,
    DO_NOT_DISTURB,
}

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val phone: String? = null,
    val displayName: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val status: UserStatus = UserStatus.OFFLINE,
    val lastSeen: Long? = null,
    val isVerified: Boolean = false,
)

@Serializable
enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    AUDIO,
    FILE,
    STICKER,
    LOCATION,
    CONTACT,
    POLL,
    CALL,
}

@Serializable
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED,
}

@Serializable
data class Message(
    val id: String,
    val chatId: String,
    val senderId: String,
    val type: MessageType = MessageType.TEXT,
    val content: String? = null,
    val mediaUrl: String? = null,
    val mediaSize: Long? = null,
    val mediaMime: String? = null,
    val thumbUrl: String? = null,
    val replyTo: Message? = null,
    val forwardFrom: Message? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    val isPinned: Boolean = false,
    val reactions: Map<String, List<String>> = emptyMap(),
    val status: MessageStatus = MessageStatus.SENT,
    val createdAt: Long,
    val editedAt: Long? = null,
)

@Serializable
enum class ChatType {
    PRIVATE,
    GROUP,
    CHANNEL,
}

@Serializable
data class Chat(
    val id: String,
    val type: ChatType,
    val name: String? = null,
    val description: String? = null,
    val avatarUrl: String? = null,
    val ownerId: String? = null,
    val isPublic: Boolean = false,
    val memberCount: Int = 0,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
    val isMuted: Boolean = false,
    val isPinned: Boolean = false,
    val inviteLink: String? = null,
    val createdAt: Long,
)

@Serializable
enum class ChatRole {
    OWNER,
    ADMIN,
    MEMBER,
}

@Serializable
data class ChatMember(
    val chatId: String,
    val userId: String,
    val role: ChatRole = ChatRole.MEMBER,
    val joinedAt: Long,
    val mutedUntil: Long? = null,
    val lastReadId: String? = null,
)

@Serializable
data class DeviceSession(
    val id: String,
    val userId: String,
    val deviceName: String? = null,
    val deviceType: String? = null,
    val pushToken: String? = null,
    val lastActive: Long? = null,
    val expiresAt: Long,
)
