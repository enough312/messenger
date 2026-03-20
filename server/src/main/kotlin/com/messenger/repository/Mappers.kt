package com.messenger.repository

import com.messenger.shared.model.Chat
import com.messenger.shared.model.ChatMember
import com.messenger.shared.model.ChatRole
import com.messenger.shared.model.ChatType
import com.messenger.shared.model.Message
import com.messenger.shared.model.MessageStatus
import com.messenger.shared.model.MessageType
import com.messenger.shared.model.User
import com.messenger.shared.model.UserStatus
import com.messenger.table.ChatMembersTable
import com.messenger.table.ChatsTable
import com.messenger.table.MessagesTable
import com.messenger.table.UsersTable
import org.jetbrains.exposed.sql.ResultRow

fun ResultRow.toUser(): User = User(
    id = this[UsersTable.id].toString(),
    username = this[UsersTable.username],
    email = this[UsersTable.email],
    phone = this[UsersTable.phone],
    displayName = this[UsersTable.displayName],
    bio = this[UsersTable.bio],
    avatarUrl = this[UsersTable.avatarUrl],
    status = UserStatus.valueOf(this[UsersTable.status].uppercase()),
    lastSeen = this[UsersTable.lastSeen],
    isVerified = this[UsersTable.isVerified],
)

fun ResultRow.toChat(
    lastMessage: Message? = null,
    memberCount: Int = 0,
    unreadCount: Int = 0,
): Chat = Chat(
    id = this[ChatsTable.id].toString(),
    type = ChatType.valueOf(this[ChatsTable.type].uppercase()),
    name = this[ChatsTable.name],
    description = this[ChatsTable.description],
    avatarUrl = this[ChatsTable.avatarUrl],
    ownerId = this[ChatsTable.ownerId]?.toString(),
    isPublic = this[ChatsTable.isPublic],
    memberCount = memberCount,
    lastMessage = lastMessage,
    unreadCount = unreadCount,
    inviteLink = this[ChatsTable.inviteLink],
    createdAt = this[ChatsTable.createdAt],
)

fun ResultRow.toChatMember(): ChatMember = ChatMember(
    chatId = this[ChatMembersTable.chatId].toString(),
    userId = this[ChatMembersTable.userId].toString(),
    role = ChatRole.valueOf(this[ChatMembersTable.role].uppercase()),
    joinedAt = this[ChatMembersTable.joinedAt],
    mutedUntil = this[ChatMembersTable.mutedUntil],
    lastReadId = this[ChatMembersTable.lastReadId]?.toString(),
)

fun ResultRow.toMessage(reactions: Map<String, List<String>> = emptyMap()): Message = Message(
    id = this[MessagesTable.id].toString(),
    chatId = this[MessagesTable.chatId].toString(),
    senderId = this[MessagesTable.senderId].toString(),
    type = MessageType.valueOf(this[MessagesTable.type].uppercase()),
    content = this[MessagesTable.content],
    mediaUrl = this[MessagesTable.mediaUrl],
    mediaSize = this[MessagesTable.mediaSize],
    mediaMime = this[MessagesTable.mediaMime],
    thumbUrl = this[MessagesTable.thumbUrl],
    isEdited = this[MessagesTable.isEdited],
    isDeleted = this[MessagesTable.isDeleted],
    isPinned = this[MessagesTable.isPinned],
    reactions = reactions,
    status = MessageStatus.SENT,
    createdAt = this[MessagesTable.createdAt],
    editedAt = this[MessagesTable.editedAt],
)
