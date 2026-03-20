package com.messenger.desktop.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.DesktopAppState
import com.messenger.desktop.ui.components.InputBar
import com.messenger.desktop.ui.components.MessageBubble

@Composable
fun ChatPanel(
    state: DesktopAppState,
    chatId: String,
) {
    val currentUserId = state.currentUser?.id
    val selectedChat = state.chats.firstOrNull { it.id == chatId }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(selectedChat?.name ?: "Private chat")
        LazyColumn(modifier = Modifier.weight(1f, fill = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages, key = { it.id }) { message ->
                MessageBubble(message, isOwn = currentUserId != null && message.senderId == currentUserId)
            }
        }
        InputBar(
            enabled = !state.isBusy,
            onSend = state::sendMessage,
        )
    }
}
