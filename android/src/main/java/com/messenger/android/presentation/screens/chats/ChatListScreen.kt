package com.messenger.android.presentation.screens.chats

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun ChatListScreen(
    onOpenChat: (String) -> Unit,
    viewModel: ChatListViewModel = hiltViewModel(),
) {
    val chats by viewModel.chats.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(chats) { chat ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onOpenChat(chat.id) }) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(chat.name ?: chat.id)
                    Text("members: ${chat.memberCount}")
                }
            }
        }
    }
}
