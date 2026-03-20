package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object MessagesTable : Table("messages") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val chatId = uuid("chat_id").references(ChatsTable.id)
    val senderId = uuid("sender_id").references(UsersTable.id)
    val type = varchar("type", 16).default("text")
    val content = text("content").nullable()
    val mediaUrl = text("media_url").nullable()
    val mediaSize = long("media_size").nullable()
    val mediaMime = varchar("media_mime", 64).nullable()
    val thumbUrl = text("thumb_url").nullable()
    val replyToId = uuid("reply_to_id").nullable()
    val forwardFrom = uuid("forward_from").nullable()
    val isEdited = bool("is_edited").default(false)
    val isDeleted = bool("is_deleted").default(false)
    val isPinned = bool("is_pinned").default(false)
    val metadata = text("metadata").nullable()
    val createdAt = long("created_at")
    val editedAt = long("edited_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
