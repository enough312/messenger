package com.messenger.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.ui.panels.ChatPanel
import com.messenger.desktop.ui.panels.InfoPanel
import com.messenger.desktop.ui.panels.SidebarPanel
import org.koin.core.Koin

@Composable
fun MainWindow(koin: Koin) {
    var selectedChatId by remember { mutableStateOf<String?>(null) }
    Row(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            SidebarPanel(
                onSelectChat = { selectedChatId = it },
            )
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedChatId != null) {
                ChatPanel(selectedChatId!!)
            } else {
                Text("Select a chat", modifier = Modifier.padding(24.dp))
            }
        }
        Divider(modifier = Modifier.fillMaxHeight().width(1.dp))
        InfoPanel(selectedChatId)
    }
}
