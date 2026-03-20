package com.messenger.config

import com.messenger.repository.ChatRepository
import com.messenger.repository.MessageRepository
import com.messenger.repository.UserRepository
import com.messenger.service.AuthService
import com.messenger.service.CallService
import com.messenger.service.ChatService
import com.messenger.service.EmailService
import com.messenger.service.MediaService
import com.messenger.service.MessageService
import com.messenger.service.PushService
import com.messenger.service.SignalKeyService
import com.messenger.service.TwoFactorService
import com.messenger.service.UserService
import com.messenger.websocket.ConnectionManager
import com.zaxxer.hikari.HikariDataSource
import io.lettuce.core.RedisClient
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import software.amazon.awssdk.services.s3.S3Client

data class AppGraph(
    val config: AppConfig,
    val dataSource: HikariDataSource,
    val redisClient: RedisClient,
    val s3Client: S3Client?,
    val registry: PrometheusMeterRegistry,
    val connectionManager: ConnectionManager,
    val userRepository: UserRepository,
    val chatRepository: ChatRepository,
    val messageRepository: MessageRepository,
    val emailService: EmailService,
    val pushService: PushService,
    val twoFactorService: TwoFactorService,
    val signalKeyService: SignalKeyService,
    val authService: AuthService,
    val userService: UserService,
    val chatService: ChatService,
    val messageService: MessageService,
    val mediaService: MediaService,
    val callService: CallService,
)
