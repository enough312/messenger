package com.messenger.desktop.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.ConnectionState
import com.messenger.desktop.state.DesktopAppState
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.PersonAvatar
import com.messenger.desktop.ui.formatChatListTime
import com.messenger.desktop.ui.previewMessage
import com.messenger.shared.model.Chat
import com.messenger.shared.model.MessageType
import com.messenger.shared.model.User

@Composable
fun SidebarPanel(
    state: DesktopAppState,
    onSelectChat: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PersonAvatar(
                    label = state.currentUser?.displayName ?: "Messenger",
                    size = 50.dp,
                    online = state.connectionState == ConnectionState.CONNECTED,
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = state.currentUser?.displayName ?: "Unknown user",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = state.currentUser?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MessengerColors.TextMuted,
                    )
                }
                ConnectionBadge(state.connectionState)
            }
        }

        OutlinedTextField(
            value = state.userSearchQuery,
            onValueChange = {
                state.userSearchQuery = it
                state.searchUsers()
            },
            label = { Text("Search users or chats") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "People",
                        color = MessengerColors.TextMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                items(state.searchResults, key = { "search-${it.id}" }) { user ->
                    SearchUserCard(user) { state.createPrivateChat(user.id) }
                }
            }

            if (state.chats.isNotEmpty()) {
                item {
                    Text(
                        text = if (state.searchResults.isEmpty()) "Chats" else "Recent chats",
                        color = MessengerColors.TextMuted,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                items(state.chats, key = { it.id }) { chat ->
                    ChatCard(
                        chat = chat,
                        selected = state.selectedChatId == chat.id,
                        onClick = { onSelectChat(chat.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchUserCard(
    user: User,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(label = user.displayName)
            Column {
                Text(user.displayName, fontWeight = FontWeight.SemiBold)
                Text("@${user.username}", color = MessengerColors.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ChatCard(
    chat: Chat,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val lastMessage = chat.lastMessage
    val lastMessageText = when (lastMessage?.type) {
        null -> "No messages yet"
        MessageType.TEXT -> lastMessage.content ?: "No messages yet"
        else -> "[${lastMessage.type.name.lowercase()}]"
    }
    val containerColor = if (selected) MessengerColors.AccentSoft else MaterialTheme.colorScheme.surface

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = containerColor,
        shape = RoundedCornerShape(22.dp),
        tonalElevation = if (selected) 1.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PersonAvatar(label = chat.name ?: "Private chat", online = false)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = chat.name ?: "Private chat",
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = formatChatListTime(lastMessage?.createdAt ?: chat.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MessengerColors.TextMuted,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = previewMessage(lastMessageText, "No messages yet"),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MessengerColors.TextMuted,
                    )
                    if (chat.unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .background(MessengerColors.Accent, CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = chat.unreadCount.toString(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionBadge(state: ConnectionState) {
    val (label, background) = when (state) {
        ConnectionState.CONNECTED -> "Online" to MessengerColors.Success
        ConnectionState.CONNECTING -> "Syncing" to Color(0xFFFFB020)
        ConnectionState.DISCONNECTED -> "Offline" to Color(0xFFFF6B6B)
    }
    Box(
        modifier = Modifier
            .background(background.copy(alpha = 0.15f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(text = label, color = background, style = MaterialTheme.typography.labelMedium)
    }
}
