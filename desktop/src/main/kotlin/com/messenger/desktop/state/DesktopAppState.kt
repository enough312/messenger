package com.messenger.desktop.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.messenger.desktop.data.DesktopClient
import com.messenger.desktop.data.DesktopClientException
import com.messenger.desktop.data.DesktopRealtimeEvent
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.model.Chat
import com.messenger.shared.model.Message
import com.messenger.shared.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopAppState(
    private val client: DesktopClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var realtimeJob: Job? = null

    var baseUrl by mutableStateOf("https://messenger-server-5kfw.onrender.com")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var username by mutableStateOf("")
    var displayName by mutableStateOf("")
    var authMode by mutableStateOf(AuthMode.LOGIN)
    var accessToken by mutableStateOf<String?>(null)
    var refreshToken by mutableStateOf<String?>(null)
    var currentUser by mutableStateOf<User?>(null)
    var selectedChatId by mutableStateOf<String?>(null)
    var userSearchQuery by mutableStateOf("")
    var isBusy by mutableStateOf(false)
    var infoMessage by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf<String?>(null)
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)

    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateListOf<Message>()
    val searchResults = mutableStateListOf<User>()

    fun submitAuth() {
        launchBusy {
            when (authMode) {
                AuthMode.LOGIN -> {
                    val token = client.login(
                        baseUrl,
                        LoginRequest(
                            email = email.trim(),
                            password = password,
                            deviceName = "Desktop",
                            deviceType = "desktop",
                        ),
                    )
                    onLoginSuccess(token)
                }

                AuthMode.REGISTER -> {
                    val response = client.register(
                        baseUrl,
                        RegisterRequest(
                            username = username.trim(),
                            email = email.trim(),
                            password = password,
                            displayName = displayName.trim().ifBlank { username.trim() },
                        ),
                    )
                    infoMessage = response.message
                    password = ""
                    authMode = AuthMode.LOGIN
                }
            }
        }
    }

    fun loadChats() {
        val token = accessToken ?: return
        launchBusy(clearMessages = false) {
            val loaded = client.chats(baseUrl, token)
            chats.replaceAll(loaded)
            if (selectedChatId == null) {
                selectedChatId = chats.firstOrNull()?.id
            }
            selectedChatId?.let { loadMessages(it) }
        }
    }

    fun loadMessages(chatId: String) {
        val token = accessToken ?: return
        selectedChatId = chatId
        launchBusy(clearMessages = false) {
            messages.replaceAll(client.messages(baseUrl, token, chatId))
        }
    }

    fun sendMessage(content: String) {
        val chatId = selectedChatId ?: return
        val token = accessToken ?: return
        if (content.isBlank()) return
        launchBusy(clearMessages = false) {
            val message = client.sendMessage(baseUrl, token, chatId, content)
            upsertMessage(message)
            refreshChats(token)
        }
    }

    fun searchUsers() {
        val token = accessToken ?: return
        val query = userSearchQuery.trim()
        if (query.isBlank()) {
            searchResults.clear()
            return
        }
        launchBusy(clearMessages = false) {
            searchResults.replaceAll(client.searchUsers(baseUrl, token, query))
        }
    }

    fun createPrivateChat(peerUserId: String) {
        val token = accessToken ?: return
        launchBusy(clearMessages = false) {
            val chat = client.createPrivateChat(baseUrl, token, peerUserId)
            refreshChats(token)
            selectedChatId = chat.id
            messages.replaceAll(client.messages(baseUrl, token, chat.id))
            searchResults.clear()
            userSearchQuery = ""
        }
    }

    fun logout() {
        stopRealtime()
        accessToken = null
        refreshToken = null
        currentUser = null
        selectedChatId = null
        connectionState = ConnectionState.DISCONNECTED
        chats.clear()
        messages.clear()
        searchResults.clear()
        infoMessage = "Logged out"
        errorMessage = null
    }

    private suspend fun onLoginSuccess(token: TokenResponse) {
        accessToken = token.accessToken
        refreshToken = token.refreshToken
        currentUser = client.me(baseUrl, token.accessToken)
        startRealtime(token.accessToken)
        refreshChats(token.accessToken)
        selectedChatId?.let { loadMessages(it) }
    }

    private fun launchBusy(clearMessages: Boolean = true, block: suspend () -> Unit) {
        scope.launch {
            isBusy = true
            if (clearMessages) {
                infoMessage = null
                errorMessage = null
            }
            runCatching { block() }
                .onFailure { throwable ->
                    errorMessage = throwable.message ?: "Something went wrong"
                    if (throwable is DesktopClientException && throwable.status.value == 401) {
                        stopRealtime()
                        accessToken = null
                        connectionState = ConnectionState.DISCONNECTED
                    }
                }
            isBusy = false
        }
    }

    private fun startRealtime(token: String) {
        stopRealtime()
        realtimeJob = scope.launch(Dispatchers.IO) {
            var reconnectDelayMs = 1_000L
            while (isActive && accessToken == token) {
                withContext(Dispatchers.Main) {
                    connectionState = ConnectionState.CONNECTING
                }
                val result = runCatching {
                    client.runRealtimeSession(baseUrl, token) { event ->
                        withContext(Dispatchers.Main) {
                            handleRealtimeEvent(token, event)
                        }
                    }
                }
                if (!isActive || accessToken != token) break
                withContext(Dispatchers.Main) {
                    connectionState = ConnectionState.DISCONNECTED
                    result.exceptionOrNull()?.message
                        ?.takeIf { it.isNotBlank() }
                        ?.let { errorMessage = it }
                }
                delay(reconnectDelayMs)
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30_000L)
            }
        }
    }

    private fun stopRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
    }

    private suspend fun handleRealtimeEvent(token: String, event: DesktopRealtimeEvent) {
        when (event) {
            DesktopRealtimeEvent.Connected -> {
                connectionState = ConnectionState.CONNECTED
                errorMessage = null
            }

            is DesktopRealtimeEvent.Error -> {
                errorMessage = event.message
            }

            is DesktopRealtimeEvent.NewMessage -> {
                upsertMessage(event.message)
                refreshChats(token)
            }
        }
    }

    private suspend fun refreshChats(token: String) {
        val loaded = client.chats(baseUrl, token)
        chats.replaceAll(loaded)
        val currentSelection = selectedChatId
        if (currentSelection == null) {
            selectedChatId = chats.firstOrNull()?.id
        } else if (chats.none { it.id == currentSelection }) {
            selectedChatId = chats.firstOrNull()?.id
        }
    }

    private fun upsertMessage(message: Message) {
        if (message.chatId != selectedChatId) return
        val existingIndex = messages.indexOfFirst { it.id == message.id }
        if (existingIndex >= 0) {
            messages[existingIndex] = message
        } else {
            messages.add(message)
        }
    }

    private fun <T> MutableList<T>.replaceAll(newItems: List<T>) {
        clear()
        addAll(newItems)
    }
}

enum class AuthMode {
    LOGIN,
    REGISTER,
}

enum class ConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
}
