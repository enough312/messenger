package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object CallsTable : Table("calls") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val chatId = uuid("chat_id").references(ChatsTable.id)
    val callerId = uuid("caller_id").references(UsersTable.id)
    val type = varchar("type", 16)
    val status = varchar("status", 16).default("started")
    val createdAt = long("created_at")
    val endedAt = long("ended_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
