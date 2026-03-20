package com.messenger.desktop.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.DesktopAppState
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.PersonAvatar
import com.messenger.desktop.ui.components.InputBar
import com.messenger.desktop.ui.components.MessageBubble

@Composable
fun ChatPanel(
    state: DesktopAppState,
    chatId: String,
) {
    val currentUserId = state.currentUser?.id
    val selectedChat = state.chats.firstOrNull { it.id == chatId }
    val listState = rememberLazyListState()

    LaunchedEffect(chatId, state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MessengerColors.SurfaceSoft),
            verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(
                    label = selectedChat?.name ?: "Private chat",
                    size = 52.dp,
                    online = state.connectionState == com.messenger.desktop.state.ConnectionState.CONNECTED,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = selectedChat?.name ?: "Private chat",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    AnimatedVisibility(visible = state.typingHint != null) {
                        Text(
                            text = state.typingHint ?: "",
                            color = MessengerColors.Accent,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    AnimatedVisibility(visible = state.typingHint == null) {
                        Text(
                            text = selectedChat?.description ?: "Live conversation",
                            color = MessengerColors.TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                Text(
                    text = "${state.messages.size} messages",
                    color = MessengerColors.TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOwn = currentUserId != null && message.senderId == currentUserId,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            InputBar(
                chatId = chatId,
                enabled = !state.isBusy,
                onTypingChanged = state::onComposerChanged,
                onSend = state::sendMessage,
            )
        }
    }
}
