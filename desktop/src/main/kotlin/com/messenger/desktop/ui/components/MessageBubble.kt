package com.messenger.desktop.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.formatMessageTime
import com.messenger.desktop.ui.openExternalUrl
import com.messenger.shared.model.Message
import com.messenger.shared.model.MessageStatus
import com.messenger.shared.model.MessageType

@Composable
fun MessageBubble(
    message: Message,
    isOwn: Boolean,
    onReact: (String, String) -> Unit,
) {
    val bubbleColor = if (isOwn) MessengerColors.Accent else MessengerColors.BubbleOther
    val textColor = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface
    val metaColor = if (isOwn) Color(0xFFE5DBFF) else MessengerColors.TextMuted
    val bubbleShape = if (isOwn) {
        RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentWidth(if (isOwn) Alignment.End else Alignment.Start),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .background(bubbleColor, bubbleShape)
                .hoverable(interactionSource)
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

            MediaPayload(
                message = message,
                textColor = textColor,
                metaColor = metaColor,
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

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .wrapContentSize(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                message.reactions
                    .toSortedMap()
                    .forEach { (emoji, users) ->
                        ReactionChip(emoji = emoji, count = users.size) {
                            onReact(message.id, emoji)
                        }
                    }
            }
        }

        if (isHovered) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("👍", "❤️", "😂", "🔥").forEach { emoji ->
                    ReactionChip(emoji = emoji, count = null) {
                        onReact(message.id, emoji)
                    }
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

@Composable
private fun MediaPayload(
    message: Message,
    textColor: Color,
    metaColor: Color,
) {
    val mediaUrl = message.mediaUrl
    if (mediaUrl == null) {
        Text(
            text = message.content ?: "[${message.type.name.lowercase()}]",
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
        )
        return
    }

    val label = when {
        message.mediaMime?.startsWith("image/") == true || message.type == MessageType.IMAGE -> "Image attachment"
        message.mediaMime?.startsWith("video/") == true || message.type == MessageType.VIDEO -> "Video attachment"
        message.mediaMime?.startsWith("audio/") == true || message.type == MessageType.AUDIO -> "Audio attachment"
        else -> "File attachment"
    }

    Surface(
        modifier = Modifier.clickable { openExternalUrl(mediaUrl) },
        color = Color.White.copy(alpha = if (textColor == Color.White) 0.16f else 0.55f),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = message.content ?: mediaUrl,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Open attachment",
                color = metaColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun ReactionChip(
    emoji: String,
    count: Int?,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color.White,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = emoji, style = MaterialTheme.typography.labelMedium)
            count?.let {
                Text(
                    text = it.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MessengerColors.TextMuted,
                )
            }
        }
    }
}
