package com.messenger.repository

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

object Db {
    lateinit var database: Database

    fun <T> query(block: () -> T): T = transaction(database) { block() }
}
