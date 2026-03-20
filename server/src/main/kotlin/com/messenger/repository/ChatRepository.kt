package com.messenger.repository

import com.messenger.shared.dto.CreateChannelRequest
import com.messenger.shared.dto.CreateGroupChatRequest
import com.messenger.shared.dto.UpdateChatMemberRoleRequest
import com.messenger.shared.dto.UpdateChatRequest
import com.messenger.shared.model.Chat
import com.messenger.shared.model.ChatMember
import com.messenger.shared.model.ChatRole
import com.messenger.table.ChatMembersTable
import com.messenger.table.ChatsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ChatRepository(private val messageRepository: MessageRepository) {
    fun createPrivateChat(ownerId: String, peerId: String): Chat = Db.query {
        findExistingPrivateChat(ownerId, peerId)?.let { return@query it }

        val now = Instant.now().toEpochMilli()
        val chatId = ChatsTable.insert {
            it[type] = "private"
            it[ChatsTable.ownerId] = UUID.fromString(ownerId)
            it[isPublic] = false
            it[createdAt] = now
            it[updatedAt] = now
        } get ChatsTable.id

        listOf(ownerId, peerId).forEach { memberId ->
            ChatMembersTable.insert {
                it[ChatMembersTable.chatId] = chatId
                it[userId] = UUID.fromString(memberId)
                it[role] = if (memberId == ownerId) "owner" else "member"
                it[joinedAt] = now
            }
        }
        getChat(chatId.toString(), ownerId) ?: error("Private chat was not created")
    }

    fun createGroupChat(ownerId: String, request: CreateGroupChatRequest): Chat = Db.query {
        val now = Instant.now().toEpochMilli()
        val chatId = ChatsTable.insert {
            it[type] = "group"
            it[name] = request.name
            it[description] = request.description
            it[ChatsTable.ownerId] = UUID.fromString(ownerId)
            it[isPublic] = request.isPublic
            it[inviteLink] = UUID.randomUUID().toString().substring(0, 8)
            it[createdAt] = now
            it[updatedAt] = now
        } get ChatsTable.id

        (request.memberIds + ownerId).distinct().forEach { memberId ->
            ChatMembersTable.insertIgnore {
                it[ChatMembersTable.chatId] = chatId
                it[userId] = UUID.fromString(memberId)
                it[role] = if (memberId == ownerId) "owner" else "member"
                it[joinedAt] = now
            }
        }
        getChat(chatId.toString(), ownerId) ?: error("Group chat was not created")
    }

    fun createChannel(ownerId: String, request: CreateChannelRequest): Chat = Db.query {
        val now = Instant.now().toEpochMilli()
        val chatId = ChatsTable.insert {
            it[type] = "channel"
            it[name] = request.name
            it[description] = request.description
            it[ChatsTable.ownerId] = UUID.fromString(ownerId)
            it[isPublic] = request.isPublic
            it[inviteLink] = UUID.randomUUID().toString().substring(0, 8)
            it[createdAt] = now
            it[updatedAt] = now
        } get ChatsTable.id

        ChatMembersTable.insert {
            it[ChatMembersTable.chatId] = chatId
            it[userId] = UUID.fromString(ownerId)
            it[role] = "owner"
            it[joinedAt] = now
        }
        getChat(chatId.toString(), ownerId) ?: error("Channel was not created")
    }

    fun listChats(userId: String): List<Chat> = Db.query {
        (ChatMembersTable innerJoin ChatsTable)
            .selectAll()
            .where { ChatMembersTable.userId eq UUID.fromString(userId) }
            .withDistinct()
            .map { row ->
                val chatId = row[ChatsTable.id].toString()
                row.toChat(
                    lastMessage = messageRepository.getLatestMessage(chatId),
                    memberCount = countMembers(chatId),
                )
            }
    }

    fun getChat(chatId: String, viewerId: String): Chat? = Db.query {
        if (!isMember(chatId, viewerId)) return@query null
        ChatsTable.selectAll()
            .where { ChatsTable.id eq UUID.fromString(chatId) }
            .limit(1)
            .firstOrNull()
            ?.toChat(
                lastMessage = messageRepository.getLatestMessage(chatId),
                memberCount = countMembers(chatId),
            )
    }

    fun updateChat(chatId: String, request: UpdateChatRequest): Chat? = Db.query {
        ChatsTable.update({ ChatsTable.id eq UUID.fromString(chatId) }) {
            request.name?.let { value -> it[name] = value }
            request.description?.let { value -> it[description] = value }
            request.avatarUrl?.let { value -> it[avatarUrl] = value }
            request.isPublic?.let { value -> it[isPublic] = value }
            it[updatedAt] = Instant.now().toEpochMilli()
        }
        getChat(chatId, viewerId = findOwner(chatId) ?: return@query null)
    }

    fun deleteOrLeave(chatId: String, userId: String): Boolean = Db.query {
        if (findOwner(chatId) == userId) {
            ChatsTable.deleteWhere { id eq UUID.fromString(chatId) } > 0
        } else {
            ChatMembersTable.deleteWhere {
                (ChatMembersTable.chatId eq UUID.fromString(chatId)) and
                    (ChatMembersTable.userId eq UUID.fromString(userId))
            } > 0
        }
    }

    fun listMembers(chatId: String): List<ChatMember> = Db.query {
        ChatMembersTable.selectAll().where { ChatMembersTable.chatId eq UUID.fromString(chatId) }.map { it.toChatMember() }
    }

    fun addMember(chatId: String, userId: String, role: ChatRole): Boolean = Db.query {
        ChatMembersTable.insertIgnore {
            it[ChatMembersTable.chatId] = UUID.fromString(chatId)
            it[ChatMembersTable.userId] = UUID.fromString(userId)
            it[ChatMembersTable.role] = role.name.lowercase()
            it[joinedAt] = Instant.now().toEpochMilli()
        }.insertedCount > 0
    }

    fun removeMember(chatId: String, userId: String): Boolean = Db.query {
        ChatMembersTable.deleteWhere {
            (ChatMembersTable.chatId eq UUID.fromString(chatId)) and
                (ChatMembersTable.userId eq UUID.fromString(userId))
        } > 0
    }

    fun updateMemberRole(chatId: String, userId: String, request: UpdateChatMemberRoleRequest): Boolean = Db.query {
        ChatMembersTable.update({
            (ChatMembersTable.chatId eq UUID.fromString(chatId)) and
                (ChatMembersTable.userId eq UUID.fromString(userId))
        }) {
            it[role] = request.role.name.lowercase()
        } > 0
    }

    fun getChatMemberIds(chatId: String): List<String> = Db.query {
        ChatMembersTable.selectAll().where { ChatMembersTable.chatId eq UUID.fromString(chatId) }
            .map { it[ChatMembersTable.userId].toString() }
    }

    fun isMember(chatId: String, userId: String): Boolean = Db.query {
        ChatMembersTable.selectAll()
            .where {
                (ChatMembersTable.chatId eq UUID.fromString(chatId)) and
                    (ChatMembersTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .count() > 0
    }

    private fun countMembers(chatId: String): Int = ChatMembersTable.selectAll()
        .where { ChatMembersTable.chatId eq UUID.fromString(chatId) }
        .count()
        .toInt()

    private fun findOwner(chatId: String): String? = ChatsTable.selectAll()
        .where { ChatsTable.id eq UUID.fromString(chatId) }
        .limit(1)
        .firstOrNull()
        ?.get(ChatsTable.ownerId)
        ?.toString()

    private fun findExistingPrivateChat(ownerId: String, peerId: String): Chat? {
        val ownerUuid = UUID.fromString(ownerId)
        val peerUuid = UUID.fromString(peerId)
        val chatIds = ChatMembersTable.selectAll().where { ChatMembersTable.userId eq ownerUuid }.map { it[ChatMembersTable.chatId] }
        return chatIds.firstNotNullOfOrNull { chatId ->
            val members = ChatMembersTable.selectAll().where { ChatMembersTable.chatId eq chatId }.map { it[ChatMembersTable.userId] }
            if (members.size == 2 && members.contains(ownerUuid) && members.contains(peerUuid)) {
                getChat(chatId.toString(), ownerId)
            } else {
                null
            }
        }
    }
}
