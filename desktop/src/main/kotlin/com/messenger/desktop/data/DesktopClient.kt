package com.messenger.desktop.data

import com.messenger.shared.dto.ChatListResponse
import com.messenger.shared.dto.CreatePrivateChatRequest
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.MessageListResponse
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.dto.UserSearchResponse
import com.messenger.shared.dto.VerificationResponse
import com.messenger.shared.model.Chat
import com.messenger.shared.model.Message
import com.messenger.shared.model.User
import com.messenger.shared.util.MessengerJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

class DesktopClient {
    private val httpClient = HttpClient(CIO) {
        expectSuccess = false
        install(ContentNegotiation) {
            json(MessengerJson)
        }
    }

    suspend fun register(baseUrl: String, request: RegisterRequest): VerificationResponse {
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/auth/register") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        return response.decode()
    }

    suspend fun login(baseUrl: String, request: LoginRequest): TokenResponse {
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

    suspend fun messages(baseUrl: String, accessToken: String, chatId: String): List<Message> {
        val response = httpClient.get("${baseUrl.normalizeBaseUrl()}/chats/$chatId/messages") {
            accept(ContentType.Application.Json)
            bearerAuth(accessToken)
        }
        return response.decode<MessageListResponse>().items
    }

    suspend fun sendMessage(baseUrl: String, accessToken: String, chatId: String, content: String): Message {
        val response = httpClient.post("${baseUrl.normalizeBaseUrl()}/chats/$chatId/messages") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            bearerAuth(accessToken)
            setBody(SendMessageRequest(content = content))
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

    private fun String.normalizeBaseUrl(): String = trim().trimEnd('/')
}

@Serializable
private data class ApiErrorPayload(
    val message: String,
)

class DesktopClientException(
    val status: HttpStatusCode,
    override val message: String,
) : RuntimeException(message)
