package com.messenger.table

import org.jetbrains.exposed.sql.Table

object SignalKeysTable : Table("signal_keys") {
    val userId = uuid("user_id").references(UsersTable.id)
    val identityKey = text("identity_key")
    val signedPreKey = text("signed_pre_key")
    val oneTimePreKeys = text("one_time_pre_keys")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
