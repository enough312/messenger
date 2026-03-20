package com.messenger.table

import org.jetbrains.exposed.sql.Table

object ContactsTable : Table("contacts") {
    val ownerId = uuid("owner_id").references(UsersTable.id)
    val contactId = uuid("contact_id").references(UsersTable.id)
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(ownerId, contactId)
}
