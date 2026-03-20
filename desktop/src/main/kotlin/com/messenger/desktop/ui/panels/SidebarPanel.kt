package com.messenger.desktop.ui.panels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.DesktopAppState
import com.messenger.shared.model.Chat
import com.messenger.shared.model.User

@Composable
fun SidebarPanel(
    state: DesktopAppState,
    onSelectChat: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(state.currentUser?.displayName ?: "Unknown user")
        Text(state.currentUser?.email ?: "")
        Button(onClick = state::loadChats, enabled = !state.isBusy) {
            Text("Refresh chats")
        }
        OutlinedTextField(
            value = state.userSearchQuery,
            onValueChange = { state.userSearchQuery = it },
            label = { Text("Find users") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = state::searchUsers, enabled = !state.isBusy) {
            Text("Search")
        }
        state.searchResults.forEach { user ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { state.createPrivateChat(user.id) },
            ) {
                SearchUserCard(user)
            }
        }
        state.chats.forEach { chat ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectChat(chat.id) },
            ) {
                ChatCard(chat)
            }
        }
    }
}

@Composable
private fun SearchUserCard(user: User) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(user.displayName)
        Text("@${user.username}")
    }
}

@Composable
private fun ChatCard(chat: Chat) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(chat.name ?: "Chat ${chat.id.take(8)}")
        Text(chat.lastMessage?.content ?: "No messages yet")
    }
}
