package com.messenger.desktop.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.messenger.shared.model.Message

@Composable
fun MessageBubble(message: Message, isOwn: Boolean) {
    Text(
        text = message.content ?: "[${message.type.name.lowercase()}]",
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(if (isOwn) Alignment.End else Alignment.Start)
            .background(
                if (isOwn) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
            )
            .padding(12.dp),
    )
}
