package com.messenger.desktop.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.messenger.desktop.data.DesktopClient
import com.messenger.desktop.data.DesktopClientException
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.TokenResponse
import com.messenger.shared.model.Chat
import com.messenger.shared.model.Message
import com.messenger.shared.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DesktopAppState(
    private val client: DesktopClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

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
            messages.add(message)
            chats.replaceAll(client.chats(baseUrl, token))
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
            loadChats()
            selectedChatId = chat.id
            searchResults.clear()
            userSearchQuery = ""
        }
    }

    fun logout() {
        accessToken = null
        refreshToken = null
        currentUser = null
        selectedChatId = null
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
        loadChats()
    }

    private fun launchBusy(clearMessages: Boolean = true, block: suspend () -> Unit) {
        scope.launch {
            isBusy = true
            if (clearMessages) {
                infoMessage = null
                errorMessage = null
            }
            runCatching { withContext(Dispatchers.IO) { block() } }
                .onFailure { throwable ->
                    errorMessage = throwable.message ?: "Something went wrong"
                    if (throwable is DesktopClientException && throwable.status.value == 401) {
                        accessToken = null
                    }
                }
            isBusy = false
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
