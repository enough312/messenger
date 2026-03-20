package com.messenger.desktop.ui.panels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun InfoPanel(selectedChatId: String?) {
    Column(modifier = Modifier.width(280.dp).fillMaxHeight().padding(16.dp)) {
        Text("Info")
        Text(selectedChatId ?: "No chat selected")
    }
}
