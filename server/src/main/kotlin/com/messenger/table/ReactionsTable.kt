package com.messenger.table

import org.jetbrains.exposed.sql.Table

object ReactionsTable : Table("message_reactions") {
    val messageId = uuid("message_id").references(MessagesTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val emoji = varchar("emoji", 32)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(messageId, userId, emoji)
}
