package com.messenger.android.data.repository

import com.messenger.android.data.remote.api.ChatApi
import com.messenger.android.data.remote.websocket.WebSocketClient
import com.messenger.shared.dto.SendMessageRequest
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val chatApi: ChatApi,
    private val webSocketClient: WebSocketClient,
) {
    suspend fun chats() = chatApi.chats().items

    suspend fun messages(chatId: String) = chatApi.messages(chatId).items

    suspend fun send(chatId: String, content: String) = chatApi.send(chatId, SendMessageRequest(content = content))

    val events = webSocketClient.events
}
