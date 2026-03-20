package com.messenger.desktop.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.pickAttachmentFile
import java.io.File

@Composable
fun InputBar(
    chatId: String,
    initialText: String = "",
    resetToken: Int = 0,
    enabled: Boolean = true,
    onTypingChanged: (String) -> Unit,
    onAttach: (File) -> Unit = {},
    onSend: (String) -> Unit,
) {
    var value by remember(chatId, resetToken) { mutableStateOf(initialText) }

    fun submit() {
        val trimmed = value.trim()
        if (trimmed.isBlank()) return
        onSend(trimmed)
        value = ""
        onTypingChanged("")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            ActionCircleButton(
                label = "Attach",
                enabled = enabled,
                onClick = {
                    pickAttachmentFile()?.let { file ->
                        onAttach(file)
                    }
                },
            )
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value = it
                    onTypingChanged(it)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 56.dp, max = 140.dp)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter && !event.isShiftPressed) {
                            submit()
                            true
                        } else {
                            false
                        }
                    },
                enabled = enabled,
                maxLines = 5,
                placeholder = { Text("Message...") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                shape = RoundedCornerShape(20.dp),
            )
            ActionCircleButton(
                label = if (value.isBlank()) "Ready" else "Send",
                primary = value.isNotBlank(),
                enabled = enabled,
                onClick = { submit() },
            )
        }
    }
}

@Composable
private fun ActionCircleButton(
    label: String,
    primary: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .background(
                if (primary) MessengerColors.Accent else MessengerColors.AccentSoft,
                CircleShape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (primary) MaterialTheme.colorScheme.onPrimary else MessengerColors.Accent,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
