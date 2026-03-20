package com.messenger.repository

import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.UpdateProfileRequest
import com.messenger.shared.model.DeviceSession
import com.messenger.shared.model.User
import com.messenger.table.BlocksTable
import com.messenger.table.ContactsTable
import com.messenger.table.EmailVerificationsTable
import com.messenger.table.PasswordResetsTable
import com.messenger.table.SessionsTable
import com.messenger.table.SignalKeysTable
import com.messenger.table.UsersTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class UserRepository {
    fun createUser(request: RegisterRequest, passwordHash: String): User = Db.query {
        val now = Instant.now().toEpochMilli()
        UsersTable.insert {
            it[username] = request.username
            it[email] = request.email.lowercase()
            it[phone] = request.phone
            it[UsersTable.passwordHash] = passwordHash
            it[displayName] = request.displayName
            it[createdAt] = now
            it[updatedAt] = now
        }
        findByEmail(request.email) ?: error("User was not created")
    }

    fun findByEmail(email: String): User? = Db.query {
        UsersTable.selectAll()
            .where { UsersTable.email eq email.lowercase() }
            .limit(1)
            .firstOrNull()
            ?.toUser()
    }

    fun findCredentialsByEmail(email: String): Pair<User, String>? = Db.query {
        UsersTable.selectAll()
            .where { UsersTable.email eq email.lowercase() }
            .limit(1)
            .firstOrNull()
            ?.let { it.toUser() to it[UsersTable.passwordHash] }
    }

    fun findById(id: String): User? = Db.query {
        UsersTable.selectAll().where { UsersTable.id eq UUID.fromString(id) }.limit(1).firstOrNull()?.toUser()
    }

    fun search(query: String): List<User> = Db.query {
        val term = "%${query.lowercase()}%"
        val predicate: Op<Boolean> = (UsersTable.username.lowerCase() like term) or (UsersTable.displayName.lowerCase() like term)
        UsersTable.selectAll().where { predicate }.limit(50).map { it.toUser() }
    }

    fun updateProfile(userId: String, request: UpdateProfileRequest): User = Db.query {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            request.displayName?.let { value -> it[displayName] = value }
            request.bio?.let { value -> it[bio] = value }
            request.avatarUrl?.let { value -> it[avatarUrl] = value }
            request.phone?.let { value -> it[phone] = value }
            it[updatedAt] = Instant.now().toEpochMilli()
        }
        findById(userId) ?: error("User not found after profile update")
    }

    fun markVerified(email: String): Int = Db.query {
        UsersTable.update({ UsersTable.email eq email.lowercase() }) {
            it[isVerified] = true
            it[updatedAt] = Instant.now().toEpochMilli()
        }
    }

    fun updatePassword(email: String, passwordHash: String): Int = Db.query {
        UsersTable.update({ UsersTable.email eq email.lowercase() }) {
            it[UsersTable.passwordHash] = passwordHash
            it[updatedAt] = Instant.now().toEpochMilli()
        }
    }

    fun deleteAccount(userId: String): Int = Db.query {
        UsersTable.deleteWhere { id eq UUID.fromString(userId) }
    }

    fun addContact(ownerId: String, contactId: String) = Db.query {
        ContactsTable.insertIgnore {
            it[ContactsTable.ownerId] = UUID.fromString(ownerId)
            it[ContactsTable.contactId] = UUID.fromString(contactId)
            it[createdAt] = Instant.now().toEpochMilli()
        }
    }

    fun deleteContact(ownerId: String, contactId: String) = Db.query {
        ContactsTable.deleteWhere {
            (ContactsTable.ownerId eq UUID.fromString(ownerId)) and
                (ContactsTable.contactId eq UUID.fromString(contactId))
        }
    }

    fun listContacts(ownerId: String): List<User> = Db.query {
        (ContactsTable innerJoin UsersTable)
            .selectAll()
            .where { ContactsTable.ownerId eq UUID.fromString(ownerId) }
            .map { it.toUser() }
    }

    fun blockUser(ownerId: String, blockedUserId: String) = Db.query {
        BlocksTable.insertIgnore {
            it[BlocksTable.ownerId] = UUID.fromString(ownerId)
            it[BlocksTable.blockedUserId] = UUID.fromString(blockedUserId)
            it[createdAt] = Instant.now().toEpochMilli()
        }
    }

    fun unblockUser(ownerId: String, blockedUserId: String) = Db.query {
        BlocksTable.deleteWhere {
            (BlocksTable.ownerId eq UUID.fromString(ownerId)) and
                (BlocksTable.blockedUserId eq UUID.fromString(blockedUserId))
        }
    }

    fun createSession(userId: String, request: LoginRequest, refreshToken: String, ipAddress: String?, userAgent: String?, refreshTtlDays: Long): DeviceSession = Db.query {
        val now = Instant.now().toEpochMilli()
        val expiresAt = now + refreshTtlDays * 24 * 60 * 60 * 1000
        val sessionId = SessionsTable.insert {
            it[SessionsTable.userId] = UUID.fromString(userId)
            it[deviceName] = request.deviceName
            it[deviceType] = request.deviceType
            it[fcmToken] = request.pushToken
            it[SessionsTable.refreshToken] = refreshToken
            it[SessionsTable.ipAddress] = ipAddress
            it[SessionsTable.userAgent] = userAgent
            it[lastActive] = now
            it[createdAt] = now
            it[SessionsTable.expiresAt] = expiresAt
        } get SessionsTable.id
        DeviceSession(
            id = sessionId.toString(),
            userId = userId,
            deviceName = request.deviceName,
            deviceType = request.deviceType,
            pushToken = request.pushToken,
            lastActive = now,
            expiresAt = expiresAt,
        )
    }

    fun findSessionByRefreshToken(refreshToken: String): Pair<DeviceSession, User>? = Db.query {
        (SessionsTable innerJoin UsersTable)
            .selectAll()
            .where { SessionsTable.refreshToken eq refreshToken }
            .limit(1)
            .firstOrNull()
            ?.let { row ->
                DeviceSession(
                    id = row[SessionsTable.id].toString(),
                    userId = row[SessionsTable.userId].toString(),
                    deviceName = row[SessionsTable.deviceName],
                    deviceType = row[SessionsTable.deviceType],
                    pushToken = row[SessionsTable.fcmToken],
                    lastActive = row[SessionsTable.lastActive],
                    expiresAt = row[SessionsTable.expiresAt],
                ) to row.toUser()
            }
    }

    fun invalidateSession(refreshToken: String) = Db.query {
        SessionsTable.deleteWhere { SessionsTable.refreshToken eq refreshToken }
    }

    fun createEmailVerification(email: String, code: String, ttlMillis: Long) = Db.query {
        val now = Instant.now().toEpochMilli()
        EmailVerificationsTable.insert {
            it[EmailVerificationsTable.email] = email.lowercase()
            it[EmailVerificationsTable.code] = code
            it[expiresAt] = now + ttlMillis
            it[createdAt] = now
        }
    }

    fun consumeEmailVerification(email: String, code: String): Boolean = Db.query {
        val now = Instant.now().toEpochMilli()
        EmailVerificationsTable.deleteWhere {
            (EmailVerificationsTable.email eq email.lowercase()) and
                (EmailVerificationsTable.code eq code) and
                (EmailVerificationsTable.expiresAt greaterEq now)
        } > 0
    }

    fun createPasswordReset(email: String, code: String, ttlMillis: Long) = Db.query {
        val now = Instant.now().toEpochMilli()
        PasswordResetsTable.insert {
            it[PasswordResetsTable.email] = email.lowercase()
            it[PasswordResetsTable.code] = code
            it[expiresAt] = now + ttlMillis
            it[createdAt] = now
        }
    }

    fun consumePasswordReset(email: String, code: String): Boolean = Db.query {
        val now = Instant.now().toEpochMilli()
        PasswordResetsTable.deleteWhere {
            (PasswordResetsTable.email eq email.lowercase()) and
                (PasswordResetsTable.code eq code) and
                (PasswordResetsTable.expiresAt greaterEq now)
        } > 0
    }

    fun updatePresence(userId: String, status: String) = Db.query {
        val now = Instant.now().toEpochMilli()
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[UsersTable.status] = status
            it[lastSeen] = now
            it[updatedAt] = now
        }
    }

    fun saveTwoFactorSecret(userId: String, secret: String, enabled: Boolean) = Db.query {
        UsersTable.update({ UsersTable.id eq UUID.fromString(userId) }) {
            it[twoFaSecret] = secret
            it[twoFaEnabled] = enabled
            it[updatedAt] = Instant.now().toEpochMilli()
        }
    }

    fun getTwoFactorSecret(userId: String): Pair<String?, Boolean> = Db.query {
        UsersTable.selectAll()
            .where { UsersTable.id eq UUID.fromString(userId) }
            .limit(1)
            .firstOrNull()
            ?.let { it[UsersTable.twoFaSecret] to it[UsersTable.twoFaEnabled] }
            ?: (null to false)
    }

    fun saveSignalBundle(userId: String, identityKey: String, signedPreKey: String, oneTimePreKeys: String) = Db.query {
        SignalKeysTable.insertIgnore {
            it[SignalKeysTable.userId] = UUID.fromString(userId)
            it[SignalKeysTable.identityKey] = identityKey
            it[SignalKeysTable.signedPreKey] = signedPreKey
            it[SignalKeysTable.oneTimePreKeys] = oneTimePreKeys
            it[updatedAt] = Instant.now().toEpochMilli()
        }
        SignalKeysTable.update({ SignalKeysTable.userId eq UUID.fromString(userId) }) {
            it[SignalKeysTable.identityKey] = identityKey
            it[SignalKeysTable.signedPreKey] = signedPreKey
            it[SignalKeysTable.oneTimePreKeys] = oneTimePreKeys
            it[updatedAt] = Instant.now().toEpochMilli()
        }
    }

    fun getSignalBundle(userId: String): Map<String, String>? = Db.query {
        SignalKeysTable.selectAll()
            .where { SignalKeysTable.userId eq UUID.fromString(userId) }
            .limit(1)
            .firstOrNull()
            ?.let {
                mapOf(
                    "identityKey" to it[SignalKeysTable.identityKey],
                    "signedPreKey" to it[SignalKeysTable.signedPreKey],
                    "oneTimePreKeys" to it[SignalKeysTable.oneTimePreKeys],
                )
            }
    }
}
