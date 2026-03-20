package com.messenger.desktop.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.ui.components.InputBar
import com.messenger.desktop.ui.components.MessageBubble

@Composable
fun ChatPanel(chatId: String) {
    var messages by remember(chatId) { mutableStateOf(List(8) { "Message ${it + 1} in $chatId" }) }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Chat: $chatId")
        LazyColumn(modifier = Modifier.weight(1f, fill = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { message ->
                MessageBubble(message)
            }
        }
        InputBar(
            onSend = { text ->
                if (text.isNotBlank()) messages = messages + text
            },
        )
    }
}
