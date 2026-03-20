package com.messenger.table

import org.jetbrains.exposed.sql.Table

object ChatMembersTable : Table("chat_members") {
    val chatId = uuid("chat_id").references(ChatsTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val role = varchar("role", 16).default("member")
    val joinedAt = long("joined_at")
    val mutedUntil = long("muted_until").nullable()
    val lastReadId = uuid("last_read_id").nullable()

    override val primaryKey = PrimaryKey(chatId, userId)
}
