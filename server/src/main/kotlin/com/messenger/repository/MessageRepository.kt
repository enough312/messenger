package com.messenger.repository

import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.dto.UpdateMessageRequest
import com.messenger.shared.model.Message
import com.messenger.table.ChatMembersTable
import com.messenger.table.MessageReadsTable
import com.messenger.table.MessagesTable
import com.messenger.table.ReactionsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class MessageRepository {
    fun listMessages(chatId: String, userId: String, cursor: String?, limit: Int): List<Message> = Db.query {
        if (!isMember(chatId, userId)) return@query emptyList()
        val query = MessagesTable.selectAll().where {
            if (cursor == null) {
                MessagesTable.chatId eq UUID.fromString(chatId)
            } else {
                (MessagesTable.chatId eq UUID.fromString(chatId)) and (MessagesTable.createdAt less cursor.toLong())
            }
        }
        query.orderBy(MessagesTable.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { row -> row.toMessage(reactionsForMessage(row[MessagesTable.id].toString())) }
            .reversed()
    }

    fun sendMessage(chatId: String, senderId: String, request: SendMessageRequest): Message = Db.query {
        val now = Instant.now().toEpochMilli()
        val messageId = MessagesTable.insert {
            it[MessagesTable.chatId] = UUID.fromString(chatId)
            it[MessagesTable.senderId] = UUID.fromString(senderId)
            it[type] = request.type.name.lowercase()
            it[content] = request.content
            it[mediaUrl] = request.mediaUrl
            it[mediaSize] = request.mediaSize
            it[mediaMime] = request.mediaMime
            it[thumbUrl] = request.thumbUrl
            it[replyToId] = request.replyToId?.let(UUID::fromString)
            it[createdAt] = now
        } get MessagesTable.id
        getById(messageId.toString()) ?: error("Message was not created")
    }

    fun getById(messageId: String): Message? = Db.query {
        MessagesTable.selectAll()
            .where { MessagesTable.id eq UUID.fromString(messageId) }
            .limit(1)
            .firstOrNull()
            ?.toMessage(reactionsForMessage(messageId))
    }

    fun getLatestMessage(chatId: String): Message? = Db.query {
        MessagesTable.selectAll()
            .where { MessagesTable.chatId eq UUID.fromString(chatId) }
            .orderBy(MessagesTable.createdAt, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.let { row -> row.toMessage(reactionsForMessage(row[MessagesTable.id].toString())) }
    }

    fun updateMessage(messageId: String, request: UpdateMessageRequest): Message? = Db.query {
        MessagesTable.update({ MessagesTable.id eq UUID.fromString(messageId) }) {
            it[content] = request.content
            it[isEdited] = true
            it[editedAt] = Instant.now().toEpochMilli()
        }
        getById(messageId)
    }

    fun deleteMessage(messageId: String): Boolean = Db.query {
        MessagesTable.update({ MessagesTable.id eq UUID.fromString(messageId) }) {
            it[isDeleted] = true
            it[content] = null
            it[editedAt] = Instant.now().toEpochMilli()
        } > 0
    }

    fun setPinned(messageId: String, pinned: Boolean): Message? = Db.query {
        MessagesTable.update({ MessagesTable.id eq UUID.fromString(messageId) }) {
            it[isPinned] = pinned
        }
        getById(messageId)
    }

    fun addReaction(messageId: String, userId: String, emoji: String): Message? = Db.query {
        ReactionsTable.insertIgnore {
            it[ReactionsTable.messageId] = UUID.fromString(messageId)
            it[ReactionsTable.userId] = UUID.fromString(userId)
            it[ReactionsTable.emoji] = emoji
            it[createdAt] = Instant.now().toEpochMilli()
        }
        getById(messageId)
    }

    fun markRead(messageId: String, userId: String): Unit = Db.query {
        MessageReadsTable.insertIgnore {
            it[MessageReadsTable.messageId] = UUID.fromString(messageId)
            it[MessageReadsTable.userId] = UUID.fromString(userId)
            it[readAt] = Instant.now().toEpochMilli()
        }
    }

    private fun reactionsForMessage(messageId: String): Map<String, List<String>> {
        return ReactionsTable.selectAll()
            .where { ReactionsTable.messageId eq UUID.fromString(messageId) }
            .groupBy { it[ReactionsTable.emoji] }
            .mapValues { (_, rows) -> rows.map { it[ReactionsTable.userId].toString() } }
    }

    private fun isMember(chatId: String, userId: String): Boolean {
        return ChatMembersTable.selectAll()
            .where {
                (ChatMembersTable.chatId eq UUID.fromString(chatId)) and
                    (ChatMembersTable.userId eq UUID.fromString(userId))
            }
            .limit(1)
            .count() > 0
    }
}
