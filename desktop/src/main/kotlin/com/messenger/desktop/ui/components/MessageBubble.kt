package com.messenger.desktop.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.formatMessageTime
import com.messenger.shared.model.Message
import com.messenger.shared.model.MessageStatus

@Composable
fun MessageBubble(message: Message, isOwn: Boolean) {
    val bubbleColor = if (isOwn) MessengerColors.Accent else MessengerColors.BubbleOther
    val textColor = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface
    val metaColor = if (isOwn) Color(0xFFE5DBFF) else MessengerColors.TextMuted
    val bubbleShape = if (isOwn) {
        RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(if (isOwn) Alignment.End else Alignment.Start),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .background(bubbleColor, bubbleShape)
                .animateContentSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            message.replyTo?.let { reply ->
                Column(
                    modifier = Modifier
                        .background(
                            if (isOwn) Color(0x33FFFFFF) else Color(0x10000000),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = "Reply",
                        color = metaColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = reply.content ?: "[${reply.type.name.lowercase()}]",
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                }
            }

            Text(
                text = message.content ?: "[${message.type.name.lowercase()}]",
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
            )

            Row(
                modifier = Modifier.align(Alignment.End),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (message.isEdited) {
                    Text(
                        text = "edited",
                        color = metaColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    text = formatMessageTime(message.createdAt),
                    color = metaColor,
                    style = MaterialTheme.typography.labelSmall,
                )
                if (isOwn) {
                    Text(
                        text = messageStatusGlyph(message.status),
                        color = metaColor,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}

private fun messageStatusGlyph(status: MessageStatus): String = when (status) {
    MessageStatus.SENDING -> "..."
    MessageStatus.SENT -> "v"
    MessageStatus.DELIVERED -> "vv"
    MessageStatus.READ -> "VV"
    MessageStatus.FAILED -> "!"
}
