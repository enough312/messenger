package com.messenger.desktop.ui.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.DesktopAppState

@Composable
fun InfoPanel(state: DesktopAppState) {
    Column(modifier = Modifier.width(280.dp).fillMaxHeight().padding(16.dp)) {
        Text("Info")
        Text(state.currentUser?.displayName ?: "Guest")
        Text(state.currentUser?.email ?: "Not authenticated")
        Text(state.selectedChatId ?: "No chat selected")
        state.infoMessage?.let { Text(it) }
        state.errorMessage?.let { Text(it) }
    }
}
