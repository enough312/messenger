package com.messenger.desktop.data

import com.messenger.shared.dto.ChatListResponse
import com.messenger.shared.dto.CreatePrivateChatRequest
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.MediaUploadResponse
import com.messenger.shared.dto.ReadMessageRequest
import com.messenger.shared.dto.MessageListResponse
import com.messenger.shared.dto.ReactionRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.dto.UserSearchResponse
import com.messenger.shared.dto.VerificationResponse
import com.messenger.shared.model.Chat
import com.messenger.shared.model.Message
import com.messenger.shared.model.User
import com.messenger.shared.util.MessengerJson
import com.messenger.shared.ws.WsEnvelope
import com.messenger.shared.ws.WsTypes
import com.messenger.shared.ws.wsEnvelope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.client.plugins.websocket.webSocketSession
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.file.Files

class DesktopClient {
    @Volatile
    private var realtimeSession: DefaultClientWebSocketSession? = null

    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 180_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 180_000
        }
        install(ContentNegotiation) {
            json(MessengerJson)
        }
        install(WebSockets)
    }

    suspend fun register(baseUrl: String, request: RegisterRequest): VerificationResponse {
        wakeUp(baseUrl)
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/auth/register") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.decode()
    }

    suspend fun login(baseUrl: String, request: LoginRequest): TokenResponse {
        wakeUp(baseUrl)
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/auth/login") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.decode()
    }

    suspend fun me(baseUrl: String, accessToken: String): User {
        val response = httpClient.get("${baseUrl.normalizeBaseUrl()}/users/me") {
            accept(ContentType.Application.Json)
            bearerAuth(accessToken)
        }
        return response.decode()
    }

    suspend fun chats(baseUrl: String, accessToken: String): List<Chat> {
        val response = httpClient.get("${baseUrl.normalizeBaseUrl()}/chats") {
            accept(ContentType.Application.Json)
            bearerAuth(accessToken)
        }
        return response.decode<ChatListResponse>().items
    }

    suspend fun messages(
        baseUrl: String,
        accessToken: String,
        chatId: String,
        cursor: String? = null,
        limit: Int = 50,
    ): MessagePage {
        val response = httpClient.get("${baseUrl.normalizeBaseUrl()}/chats/$chatId/messages") {
            accept(ContentType.Application.Json)
            bearerAuth(accessToken)
            parameter("limit", limit)
            cursor?.let { parameter("cursor", it) }
        }
        val page = response.decode<MessageListResponse>()
        return MessagePage(items = page.items, nextCursor = page.nextCursor)
    }

    suspend fun sendMessage(baseUrl: String, accessToken: String, chatId: String, content: String): Message =
        sendMessage(
            baseUrl = baseUrl,
            accessToken = accessToken,
            chatId = chatId,
            request = SendMessageRequest(content = content),
        )

    suspend fun sendMessage(baseUrl: String, accessToken: String, chatId: String, request: SendMessageRequest): Message {
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/chats/$chatId/messages") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            bearerAuth(accessToken)
            setBody(request)
        }
        return response.decode()
    }

    suspend fun uploadMedia(baseUrl: String, accessToken: String, file: File): MediaUploadResponse {
        val contentType = Files.probeContentType(file.toPath()) ?: "application/octet-stream"
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/media/upload") {
            accept(ContentType.Application.Json)
            contentType(ContentType.parse(contentType))
            bearerAuth(accessToken)
            header("X-File-Name", file.name)
            setBody(file.readBytes())
        }
        return response.decode()
    }

    suspend fun reactToMessage(baseUrl: String, accessToken: String, messageId: String, emoji: String): Message {
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/messages/$messageId/reactions") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            bearerAuth(accessToken)
            setBody(ReactionRequest(emoji))
        }
        return response.decode()
    }

    suspend fun searchUsers(baseUrl: String, accessToken: String, query: String): List<User> {
        val response = httpClient.get("${baseUrl.normalizeBaseUrl()}/users/search?q=$query") {
            accept(ContentType.Application.Json)
            bearerAuth(accessToken)
        }
        return response.decode<UserSearchResponse>().items
    }

    suspend fun createPrivateChat(baseUrl: String, accessToken: String, peerUserId: String): Chat {
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/chats/private") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            bearerAuth(accessToken)
            setBody(CreatePrivateChatRequest(peerUserId = peerUserId))
        }
        return response.decode()
    }

    suspend fun runRealtimeSession(
        baseUrl: String,
        accessToken: String,
        onEvent: suspend (DesktopRealtimeEvent) -> Unit,
    ) {
        wakeUp(baseUrl)
        val session = httpClient.webSocketSession(urlString = baseUrl.normalizeBaseUrl().toWebSocketUrl())
        realtimeSession = session
        try {
            session.send(Frame.Text(MessengerJson.encodeToString(WsEnvelope.serializer(), wsEnvelope(
                type = WsTypes.AUTH,
                payload = buildJsonObject { put("token", accessToken) },
                json = MessengerJson,
            ))))
            coroutineScope {
                launch {
                    while (isActive) {
                        delay(20_000)
                        session.send(Frame.Text(MessengerJson.encodeToString(WsEnvelope.serializer(), wsEnvelope(
                            type = WsTypes.PING,
                            payload = JsonObject(emptyMap()),
                            json = MessengerJson,
                        ))))
                    }
                }
                for (frame in session.incoming) {
                    if (frame !is Frame.Text) continue
                    val envelope = MessengerJson.decodeFromString(WsEnvelope.serializer(), frame.readText())
                    when (envelope.type) {
                        WsTypes.AUTH_OK -> onEvent(DesktopRealtimeEvent.Connected)
                        WsTypes.NEW_MESSAGE -> onEvent(
                            DesktopRealtimeEvent.NewMessage(
                                MessengerJson.decodeFromJsonElement(Message.serializer(), envelope.payload),
                            ),
                        )
                        WsTypes.TYPING -> {
                            val chatId = envelope.payload["chatId"]?.toString()?.trim('"') ?: continue
                            val userId = envelope.payload["userId"]?.toString()?.trim('"') ?: continue
                            onEvent(
                                DesktopRealtimeEvent.Typing(
                                    chatId = chatId,
                                    userId = userId,
                                    isTyping = envelope.payload["action"]?.toString()?.trim('"') == "start",
                                ),
                            )
                        }
                        WsTypes.REACTION_ADDED -> {
                            val messageId = envelope.payload["messageId"]?.toString()?.trim('"') ?: continue
                            val emoji = envelope.payload["emoji"]?.toString()?.trim('"') ?: continue
                            val userId = envelope.payload["userId"]?.toString()?.trim('"') ?: continue
                            onEvent(DesktopRealtimeEvent.ReactionAdded(messageId, emoji, userId))
                        }
                        WsTypes.ERROR -> onEvent(
                            DesktopRealtimeEvent.Error(
                                envelope.payload["message"]?.toString()?.trim('"') ?: "WebSocket error",
                            ),
                        )
                    }
                }
            }
        } finally {
            realtimeSession = null
            session.close()
        }
    }

    suspend fun sendTyping(chatId: String, isTyping: Boolean) {
        val session = realtimeSession ?: return
        val type = if (isTyping) WsTypes.TYPING_START else WsTypes.TYPING_STOP
        session.send(
            Frame.Text(
                MessengerJson.encodeToString(
                    WsEnvelope.serializer(),
                    wsEnvelope(
                        type = type,
                        payload = buildJsonObject { put("chatId", chatId) },
                        json = MessengerJson,
                    ),
                ),
            ),
        )
    }

    suspend fun sendRead(chatId: String, messageId: String) {
        val session = realtimeSession ?: return
        session.send(
            Frame.Text(
                MessengerJson.encodeToString(
                    WsEnvelope.serializer(),
                    wsEnvelope(
                        type = WsTypes.READ_MESSAGE,
                        payload = ReadMessageRequest(chatId = chatId, messageId = messageId),
                        json = MessengerJson,
                    ),
                ),
            ),
        )
    }

    private suspend inline fun <reified T> HttpResponse.decode(): T {
        if (status.value in 200..299) return body()
        throw toDesktopClientException()
    }

    private suspend fun HttpResponse.toDesktopClientException(): DesktopClientException {
        val text = bodyAsText()
        val message = runCatching { MessengerJson.decodeFromString<ApiErrorPayload>(text).message }
            .getOrElse { text.ifBlank { status.description } }
        return DesktopClientException(status, message)
    }

    private suspend fun wakeUp(baseUrl: String) {
        runCatching {
            httpClient.get("${baseUrl.normalizeBaseUrl()}/health") {
                accept(ContentType.Application.Json)
            }
        }
    }

    private fun String.normalizeBaseUrl(): String = trim().trimEnd('/')

    private fun String.toWebSocketUrl(): String = when {
        startsWith("https://") -> replaceFirst("https://", "wss://") + "/ws"
        startsWith("http://") -> replaceFirst("http://", "ws://") + "/ws"
        else -> "ws://$this/ws"
    }
}

@Serializable
private data class ApiErrorPayload(
    val message: String,
)

class DesktopClientException(
    val status: HttpStatusCode,
    override val message: String,
) : RuntimeException(message)

data class MessagePage(
    val items: List<Message>,
    val nextCursor: String?,
)

sealed interface DesktopRealtimeEvent {
    data object Connected : DesktopRealtimeEvent
    data class NewMessage(val message: Message) : DesktopRealtimeEvent
    data class Typing(val chatId: String, val userId: String, val isTyping: Boolean) : DesktopRealtimeEvent
    data class ReactionAdded(val messageId: String, val emoji: String, val userId: String) : DesktopRealtimeEvent
    data class Error(val message: String) : DesktopRealtimeEvent
}
