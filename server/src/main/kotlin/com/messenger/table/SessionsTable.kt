package com.messenger.table

import org.jetbrains.exposed.sql.Table
import java.util.UUID

object SessionsTable : Table("sessions") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val userId = uuid("user_id").references(UsersTable.id)
    val deviceName = varchar("device_name", 128).nullable()
    val deviceType = varchar("device_type", 16).nullable()
    val fcmToken = text("fcm_token").nullable()
    val refreshToken = text("refresh_token").uniqueIndex()
    val ipAddress = varchar("ip_address", 64).nullable()
    val userAgent = text("user_agent").nullable()
    val lastActive = long("last_active")
    val createdAt = long("created_at")
    val expiresAt = long("expires_at")

    override val primaryKey = PrimaryKey(id)
}
