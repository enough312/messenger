package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object PasswordResetsTable : Table("password_resets") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val email = varchar("email", 255).index()
    val code = varchar("code", 12)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
