package com.messenger.service

import com.messenger.repository.Db
import com.messenger.table.CallsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class CallService {
    fun start(chatId: String, callerId: String, type: String): String = Db.query {
        val now = Instant.now().toEpochMilli()
        val id = CallsTable.insert {
            it[CallsTable.chatId] = UUID.fromString(chatId)
            it[CallsTable.callerId] = UUID.fromString(callerId)
            it[CallsTable.type] = type
            it[createdAt] = now
        } get CallsTable.id
        id.toString()
    }

    fun end(callId: String) = Db.query {
        CallsTable.update({ CallsTable.id eq UUID.fromString(callId) }) {
            it[status] = "ended"
            it[endedAt] = Instant.now().toEpochMilli()
        }
    }

    fun exists(callId: String): Boolean = Db.query {
        CallsTable.selectAll().where { CallsTable.id eq UUID.fromString(callId) }.limit(1).count() > 0
    }
}
