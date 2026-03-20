package com.messenger

import com.messenger.config.AppConfig
import com.messenger.config.AppGraph
import com.messenger.plugins.configureDatabases
import com.messenger.plugins.configureMonitoring
import com.messenger.plugins.configureRouting
import com.messenger.plugins.configureSecurity
import com.messenger.plugins.configureSerialization
import com.messenger.plugins.configureStatusPages
import com.messenger.plugins.configureWebSockets
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
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.lettuce.core.RedisClient
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

fun main() {
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = config.host) {
        module(config)
    }.start(wait = true)
}

fun Application.module(config: AppConfig = AppConfig.fromEnv()) {
    val graph = createGraph(config)
    configureDatabases(graph)
    configureSerialization()
    configureStatusPages()
    configureSecurity(graph)
    configureMonitoring(graph)
    configureWebSockets(graph)
    configureRouting(graph)
}

private fun createGraph(config: AppConfig): AppGraph {
    val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.dbUrl
        config.dbUser?.let { username = it }
        config.dbPassword?.let { password = it }
        maximumPoolSize = config.dbPoolSize
        driverClassName = "org.postgresql.Driver"
        config.dbSslMode?.let { addDataSourceProperty("sslmode", it) }
    }
    val dataSource = HikariDataSource(hikariConfig)
    val redisUrl = (config.redisUrl ?: buildString {
        append("redis://")
        if (!config.redisPassword.isNullOrBlank()) {
            append(":${config.redisPassword}@")
        }
        append("${config.redisHost}:${config.redisPort}")
    }).normalizeRedisUrl()
    val redisClient = RedisClient.create(redisUrl)
    val s3Client = if (config.mediaStorageMode.equals("s3", ignoreCase = true)) {
        S3Client.builder()
            .endpointOverride(URI.create(config.minioEndpoint))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.minioAccessKey, config.minioSecretKey),
                ),
            )
            .region(Region.of(config.minioRegion))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(config.s3PathStyle).build())
            .build()
    } else {
        null
    }
    val registry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    val connectionManager = ConnectionManager()

    val userRepository = UserRepository()
    val messageRepository = MessageRepository()
    val chatRepository = ChatRepository(messageRepository)
    val emailService = EmailService(config)
    val pushService = PushService(config.firebaseCredentialsPath)
    val twoFactorService = TwoFactorService(config)
    val signalKeyService = SignalKeyService(userRepository)
    val authService = AuthService(config, userRepository, emailService, twoFactorService)
    val userService = UserService(userRepository)
    val chatService = ChatService(chatRepository)
    val messageService = MessageService(chatRepository, messageRepository)
    val mediaService = MediaService(config, s3Client)
    val callService = CallService()

    return AppGraph(
        config = config,
        dataSource = dataSource,
        redisClient = redisClient,
        s3Client = s3Client,
        registry = registry,
        connectionManager = connectionManager,
        userRepository = userRepository,
        chatRepository = chatRepository,
        messageRepository = messageRepository,
        emailService = emailService,
        pushService = pushService,
        twoFactorService = twoFactorService,
        signalKeyService = signalKeyService,
        authService = authService,
        userService = userService,
        chatService = chatService,
        messageService = messageService,
        mediaService = mediaService,
        callService = callService,
    )
}

private fun String.normalizeRedisUrl(): String {
    if (startsWith("redis://") && contains("upstash.io", ignoreCase = true)) {
        return replaceFirst("redis://", "rediss://")
    }
    return this
}
