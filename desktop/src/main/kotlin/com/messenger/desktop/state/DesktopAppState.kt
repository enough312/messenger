package com.messenger.desktop.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.messenger.desktop.data.DesktopClient
import com.messenger.desktop.data.DesktopClientException
import com.messenger.desktop.data.DesktopRealtimeEvent
import com.messenger.shared.model.MessageStatus
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
    private val remoteTypingUsers = mutableStateListOf<String>()
    private val remoteTypingExpiryJobs = mutableMapOf<String, Job>()
    private val localUnreadCounts = mutableStateMapOf<String, Int>()
    private val sentReadMessageIds = mutableStateMapOf<String, Boolean>()
    private var localTypingStopJob: Job? = null
    private var localTypingChatId: String? = null
    private var messagesCursor: String? = null

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
    var hasOlderMessages by mutableStateOf(false)
    var isLoadingOlderMessages by mutableStateOf(false)

    val chats = mutableStateListOf<Chat>()
    val messages = mutableStateListOf<Message>()
    val searchResults = mutableStateListOf<User>()
    val typingUsers: List<String> get() = remoteTypingUsers
    val typingHint: String?
        get() = when {
            remoteTypingUsers.isEmpty() -> null
            remoteTypingUsers.size == 1 -> "${selectedChatTitle()} is typing..."
            else -> "Several people are typing..."
        }

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
            chats.replaceAll(applyUnreadOverlay(loaded))
            if (selectedChatId == null) {
                selectedChatId = chats.firstOrNull()?.id
            }
            selectedChatId?.let { loadMessages(it) }
        }
    }

    fun loadMessages(chatId: String) {
        val token = accessToken ?: return
        if (selectedChatId != chatId) {
            stopLocalTyping()
            clearRemoteTyping()
        }
        selectedChatId = chatId
        clearUnread(chatId)
        launchBusy(clearMessages = false) {
            val page = client.messages(baseUrl, token, chatId)
            messages.replaceAll(page.items)
            messagesCursor = page.nextCursor
            hasOlderMessages = page.nextCursor != null
            markVisibleMessagesRead(chatId)
        }
    }

    fun loadOlderMessages() {
        val token = accessToken ?: return
        val chatId = selectedChatId ?: return
        val cursor = messagesCursor ?: return
        if (isLoadingOlderMessages) return

        scope.launch {
            isLoadingOlderMessages = true
            runCatching {
                val page = client.messages(baseUrl, token, chatId, cursor = cursor)
                if (page.items.isNotEmpty()) {
                    prependMessages(page.items)
                }
                messagesCursor = page.nextCursor
                hasOlderMessages = page.nextCursor != null
            }.onFailure { throwable ->
                errorMessage = throwable.message ?: "Failed to load older messages"
            }
            isLoadingOlderMessages = false
        }
    }

    fun sendMessage(content: String) {
        val chatId = selectedChatId ?: return
        val token = accessToken ?: return
        if (content.isBlank()) return
        launchBusy(clearMessages = false) {
            val optimisticMessage = createOptimisticMessage(chatId, content)
            messages.add(optimisticMessage)
            stopLocalTyping()
            val result = runCatching { client.sendMessage(baseUrl, token, chatId, content) }
            result.onSuccess { message ->
                replaceMessage(optimisticMessage.id, message)
                updateChatLocally(message)
                refreshChats(token)
            }.onFailure { throwable ->
                replaceMessage(
                    optimisticMessage.id,
                    optimisticMessage.copy(status = MessageStatus.FAILED),
                )
                throw throwable
            }
        }
    }

    fun onComposerChanged(text: String) {
        val chatId = selectedChatId ?: return
        if (accessToken == null) return
        if (text.isBlank()) {
            stopLocalTyping()
            return
        }

        if (localTypingChatId != chatId) {
            localTypingChatId = chatId
            scope.launch(Dispatchers.IO) {
                runCatching { client.sendTyping(chatId, true) }
            }
        }

        localTypingStopJob?.cancel()
        localTypingStopJob = scope.launch {
            delay(1_800)
            stopLocalTyping()
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
            val page = client.messages(baseUrl, token, chat.id)
            messages.replaceAll(page.items)
            messagesCursor = page.nextCursor
            hasOlderMessages = page.nextCursor != null
            searchResults.clear()
            userSearchQuery = ""
        }
    }

    fun logout() {
        stopRealtime()
        stopLocalTyping()
        clearRemoteTyping()
        accessToken = null
        refreshToken = null
        currentUser = null
        selectedChatId = null
        connectionState = ConnectionState.DISCONNECTED
        chats.clear()
        messages.clear()
        searchResults.clear()
        localUnreadCounts.clear()
        sentReadMessageIds.clear()
        messagesCursor = null
        hasOlderMessages = false
        isLoadingOlderMessages = false
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
                        stopLocalTyping()
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
                    clearRemoteTyping()
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
        clearRemoteTyping()
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
                remoteTypingUsers.remove(event.message.senderId)
                remoteTypingExpiryJobs.remove(event.message.senderId)?.cancel()
                val isCurrentChat = event.message.chatId == selectedChatId
                if (isCurrentChat) {
                    upsertMessage(event.message)
                    markMessageRead(event.message)
                } else if (event.message.senderId != currentUser?.id) {
                    incrementUnread(event.message.chatId)
                }
                updateChatLocally(event.message)
                refreshChats(token)
            }

            is DesktopRealtimeEvent.Typing -> handleTypingEvent(event)
        }
    }

    private suspend fun refreshChats(token: String) {
        val loaded = client.chats(baseUrl, token)
        chats.replaceAll(applyUnreadOverlay(loaded))
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
        } else if (message.senderId == currentUser?.id) {
            val optimisticIndex = messages.indexOfFirst {
                it.id.startsWith("local-") &&
                    it.senderId == message.senderId &&
                    it.chatId == message.chatId &&
                    it.content == message.content
            }
            if (optimisticIndex >= 0) {
                messages[optimisticIndex] = message
            } else {
                messages.add(message)
            }
        } else {
            messages.add(message)
        }
    }

    private fun prependMessages(olderMessages: List<Message>) {
        if (olderMessages.isEmpty()) return
        val existingIds = messages.mapTo(mutableSetOf()) { it.id }
        val uniqueItems = olderMessages.filterNot { it.id in existingIds }
        if (uniqueItems.isEmpty()) return
        messages.addAll(0, uniqueItems)
    }

    private fun replaceMessage(oldId: String, message: Message) {
        val existingIndex = messages.indexOfFirst { it.id == oldId || it.id == message.id }
        if (existingIndex >= 0) {
            messages[existingIndex] = message
        } else {
            messages.add(message)
        }
        val duplicateIndexes = messages.withIndex()
            .filter { it.value.id == message.id }
            .map { it.index }
            .drop(1)
            .sortedDescending()
        duplicateIndexes.forEach { messages.removeAt(it) }
    }

    private fun <T> MutableList<T>.replaceAll(newItems: List<T>) {
        clear()
        addAll(newItems)
    }

    private fun handleTypingEvent(event: DesktopRealtimeEvent.Typing) {
        if (event.chatId != selectedChatId) return
        if (currentUser?.id == event.userId) return
        if (event.isTyping) {
            if (event.userId !in remoteTypingUsers) {
                remoteTypingUsers.add(event.userId)
            }
            remoteTypingExpiryJobs.remove(event.userId)?.cancel()
            remoteTypingExpiryJobs[event.userId] = scope.launch {
                delay(3_000)
                remoteTypingUsers.remove(event.userId)
                remoteTypingExpiryJobs.remove(event.userId)
            }
        } else {
            remoteTypingUsers.remove(event.userId)
            remoteTypingExpiryJobs.remove(event.userId)?.cancel()
        }
    }

    private fun stopLocalTyping() {
        val chatId = localTypingChatId ?: return
        localTypingStopJob?.cancel()
        localTypingStopJob = null
        localTypingChatId = null
        scope.launch(Dispatchers.IO) {
            runCatching { client.sendTyping(chatId, false) }
        }
    }

    private fun clearRemoteTyping() {
        remoteTypingUsers.clear()
        remoteTypingExpiryJobs.values.forEach { it.cancel() }
        remoteTypingExpiryJobs.clear()
    }

    private fun createOptimisticMessage(chatId: String, content: String): Message {
        val now = System.currentTimeMillis()
        return Message(
            id = "local-$now",
            chatId = chatId,
            senderId = currentUser?.id.orEmpty(),
            content = content,
            status = MessageStatus.SENDING,
            createdAt = now,
        )
    }

    private fun updateChatLocally(message: Message) {
        val index = chats.indexOfFirst { it.id == message.chatId }
        if (index < 0) return
        val chat = chats.removeAt(index)
        val unreadCount = if (message.chatId == selectedChatId) 0 else effectiveUnreadCount(chat)
        chats.add(
            0,
            chat.copy(
                lastMessage = message,
                unreadCount = unreadCount,
            ),
        )
    }

    private fun incrementUnread(chatId: String) {
        localUnreadCounts[chatId] = effectiveUnreadCount(chatId) + 1
        val index = chats.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            val chat = chats[index]
            chats[index] = chat.copy(unreadCount = localUnreadCounts[chatId] ?: chat.unreadCount)
        }
    }

    private fun clearUnread(chatId: String) {
        localUnreadCounts.remove(chatId)
        val index = chats.indexOfFirst { it.id == chatId }
        if (index >= 0) {
            val chat = chats[index]
            chats[index] = chat.copy(unreadCount = 0)
        }
    }

    private fun effectiveUnreadCount(chatId: String): Int {
        val localValue = localUnreadCounts[chatId]
        val serverValue = chats.firstOrNull { it.id == chatId }?.unreadCount ?: 0
        return localValue ?: serverValue
    }

    private fun effectiveUnreadCount(chat: Chat): Int = localUnreadCounts[chat.id] ?: chat.unreadCount

    private fun applyUnreadOverlay(loaded: List<Chat>): List<Chat> = loaded.map { chat ->
        val unreadCount = localUnreadCounts[chat.id] ?: chat.unreadCount
        if (unreadCount == chat.unreadCount) chat else chat.copy(unreadCount = unreadCount)
    }

    private fun markVisibleMessagesRead(chatId: String) {
        messages
            .filter { it.chatId == chatId && it.senderId != currentUser?.id && !sentReadMessageIds.containsKey(it.id) }
            .forEach { markMessageRead(it) }
    }

    private fun markMessageRead(message: Message) {
        val chatId = selectedChatId ?: return
        if (message.chatId != chatId) return
        if (message.senderId == currentUser?.id) return
        if (sentReadMessageIds.containsKey(message.id)) return
        sentReadMessageIds[message.id] = true
        clearUnread(chatId)
        scope.launch(Dispatchers.IO) {
            runCatching { client.sendRead(chatId, message.id) }
        }
    }

    private fun selectedChatTitle(): String =
        chats.firstOrNull { it.id == selectedChatId }?.name?.takeIf { it.isNotBlank() } ?: "Someone"
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
