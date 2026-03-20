package com.messenger.table

import org.jetbrains.exposed.sql.Table

object MessageReadsTable : Table("message_reads") {
    val messageId = uuid("message_id").references(MessagesTable.id)
    val userId = uuid("user_id").references(UsersTable.id)
    val readAt = long("read_at")

    override val primaryKey = PrimaryKey(messageId, userId)
}
