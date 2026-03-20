package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object ChatsTable : Table("chats") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val type = varchar("type", 16)
    val name = varchar("name", 128).nullable()
    val description = text("description").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val inviteLink = varchar("invite_link", 64).nullable().uniqueIndex()
    val ownerId = uuid("owner_id").references(UsersTable.id).nullable()
    val isPublic = bool("is_public").default(false)
    val maxMembers = integer("max_members").default(200_000)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
