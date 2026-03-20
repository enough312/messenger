package com.messenger.plugins

import com.messenger.config.AppGraph
import com.messenger.repository.Db
import com.messenger.table.BlocksTable
import com.messenger.table.CallsTable
import com.messenger.table.ChatMembersTable
import com.messenger.table.ChatsTable
import com.messenger.table.ContactsTable
import com.messenger.table.EmailVerificationsTable
import com.messenger.table.MessageReadsTable
import com.messenger.table.MessagesTable
import com.messenger.table.PasswordResetsTable
import com.messenger.table.ReactionsTable
import com.messenger.table.SessionsTable
import com.messenger.table.SignalKeysTable
import com.messenger.table.UsersTable
import io.ktor.server.application.Application
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases(graph: AppGraph) {
    val database = Database.connect(graph.dataSource)
    Db.database = database
    transaction(database) {
        SchemaUtils.createMissingTablesAndColumns(
            UsersTable,
            SessionsTable,
            ContactsTable,
            BlocksTable,
            EmailVerificationsTable,
            PasswordResetsTable,
            ChatsTable,
            ChatMembersTable,
            MessagesTable,
            MessageReadsTable,
            ReactionsTable,
            CallsTable,
            SignalKeysTable,
        )
    }
    graph.mediaService.ensureBucket()
}
