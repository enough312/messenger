package com.messenger.desktop.ui.panels

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.ConnectionState
import com.messenger.desktop.state.DesktopAppState
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.PersonAvatar

@Composable
fun InfoPanel(state: DesktopAppState) {
    val selectedChat = state.chats.firstOrNull { it.id == state.selectedChatId }
    val connectionLabel = when (state.connectionState) {
        ConnectionState.CONNECTED -> "Connected"
        ConnectionState.CONNECTING -> "Reconnecting"
        ConnectionState.DISCONNECTED -> "Offline"
    }

    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                PersonAvatar(
                    label = selectedChat?.name ?: state.currentUser?.displayName ?: "Messenger",
                    size = 86.dp,
                    online = state.connectionState == ConnectionState.CONNECTED,
                )
                Text(
                    text = selectedChat?.name ?: state.currentUser?.displayName ?: "Guest",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = selectedChat?.description ?: "Desktop messenger linked to your live server.",
                    color = MessengerColors.TextMuted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        DetailCard(
            title = "Connection",
            lines = listOf(
                "State: $connectionLabel",
                "Server: ${state.baseUrl}",
            ),
        )

        DetailCard(
            title = "Profile",
            lines = listOf(
                "Email: ${state.currentUser?.email ?: "Not authenticated"}",
                "Username: @${state.currentUser?.username ?: "unknown"}",
            ),
        )

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "Edit profile",
                    color = MessengerColors.TextMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
                OutlinedTextField(
                    value = state.profileDisplayName,
                    onValueChange = { state.profileDisplayName = it },
                    label = { Text("Display name") },
                )
                OutlinedTextField(
                    value = state.profileBio,
                    onValueChange = { state.profileBio = it },
                    label = { Text("Bio") },
                )
                OutlinedTextField(
                    value = state.profilePhone,
                    onValueChange = { state.profilePhone = it },
                    label = { Text("Phone") },
                )
                Button(
                    onClick = state::saveProfile,
                    enabled = !state.isBusy,
                ) {
                    Text("Save profile")
                }
            }
        }

        DetailCard(
            title = "Conversation",
            lines = listOf(
                "Chat: ${selectedChat?.name ?: "No chat selected"}",
                "Messages loaded: ${state.messages.size}",
                "Unread in chat list: ${selectedChat?.unreadCount ?: 0}",
            ),
        )

        state.infoMessage?.let {
            DetailCard(title = "Info", lines = listOf(it))
        }
        state.errorMessage?.let {
            DetailCard(title = "Status", lines = listOf(it))
        }
    }
}

@Composable
private fun DetailCard(title: String, lines: List<String>) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = MessengerColors.TextMuted,
                style = MaterialTheme.typography.labelLarge,
            )
            lines.forEach { line ->
                Text(text = line, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
