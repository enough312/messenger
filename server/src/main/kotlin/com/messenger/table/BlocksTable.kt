package com.messenger.table

import org.jetbrains.exposed.sql.Table

object BlocksTable : Table("user_blocks") {
    val ownerId = uuid("owner_id").references(UsersTable.id)
    val blockedUserId = uuid("blocked_user_id").references(UsersTable.id)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(ownerId, blockedUserId)
}
