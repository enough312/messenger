package com.messenger.desktop.ui.panels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
    var previousMessageCount by remember(chatId) { mutableIntStateOf(0) }
    var pendingPrependRestore by remember(chatId) { mutableStateOf<PrependRestore?>(null) }

    LaunchedEffect(chatId) {
        previousMessageCount = state.messages.size
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(state.messages.size) {
        val previousCount = previousMessageCount
        val currentCount = state.messages.size
        val delta = currentCount - previousCount
        previousMessageCount = currentCount
        if (delta <= 0 || currentCount == 0) return@LaunchedEffect

        val restore = pendingPrependRestore
        if (restore != null && state.isLoadingOlderMessages.not()) {
            pendingPrependRestore = null
            listState.scrollToItem(
                index = (restore.index + delta).coerceAtMost(state.messages.lastIndex),
                scrollOffset = restore.offset,
            )
            return@LaunchedEffect
        }

        val nearBottom = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.let { visibleIndex ->
            visibleIndex >= previousCount - 2
        } ?: true
        val latestMessage = state.messages.lastOrNull()
        val shouldStickToBottom = nearBottom || latestMessage?.senderId == currentUserId
        if (shouldStickToBottom) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    LaunchedEffect(chatId, state.hasOlderMessages, state.isLoadingOlderMessages) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index <= 1 && state.hasOlderMessages && !state.isLoadingOlderMessages && state.messages.isNotEmpty()) {
                    pendingPrependRestore = PrependRestore(index = index, offset = offset)
                    state.loadOlderMessages()
                }
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
                if (state.isLoadingOlderMessages || state.hasOlderMessages) {
                    item(key = "history-loader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (state.isLoadingOlderMessages) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(
                                text = if (state.isLoadingOlderMessages) "Loading earlier messages..." else "Scroll up for history",
                                color = MessengerColors.TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
                items(state.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        isOwn = currentUserId != null && message.senderId == currentUserId,
                        onReact = state::reactToMessage,
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
            if (state.isUploadingAttachment) {
                Text(
                    text = "Uploading attachment...",
                    color = MessengerColors.TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            InputBar(
                chatId = chatId,
                enabled = !state.isBusy && !state.isUploadingAttachment,
                onTypingChanged = state::onComposerChanged,
                onAttach = state::sendAttachment,
                onSend = state::sendMessage,
            )
        }
    }
}

private data class PrependRestore(
    val index: Int,
    val offset: Int,
)
