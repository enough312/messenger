package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object UsersTable : Table("users") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val username = varchar("username", 32).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val phone = varchar("phone", 20).nullable().uniqueIndex()
    val passwordHash = text("password_hash")
    val displayName = varchar("display_name", 64)
    val bio = text("bio").nullable()
    val avatarUrl = text("avatar_url").nullable()
    val status = varchar("status", 16).default("offline")
    val lastSeen = long("last_seen").nullable()
    val isVerified = bool("is_verified").default(false)
    val isBlocked = bool("is_blocked").default(false)
    val twoFaSecret = text("two_fa_secret").nullable()
    val twoFaEnabled = bool("two_fa_enabled").default(false)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
}
