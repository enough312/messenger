package com.messenger.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MessengerColors {
    val Accent = Color(0xFF6C5CE7)
    val AccentSoft = Color(0xFFEAE6FF)
    val Sidebar = Color(0xFFF0EFF5)
    val SurfaceSoft = Color(0xFFF7F7FB)
    val Border = Color(0xFFE5E5EA)
    val Success = Color(0xFF30D158)
    val TextMuted = Color(0xFF7F8192)
    val BubbleOther = Color(0xFFE8E8ED)
}

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM")

@Composable
fun PersonAvatar(
    label: String,
    size: Dp = 48.dp,
    online: Boolean = false,
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(colorFromLabel(label)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initials(label),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (online) {
            Box(
                modifier = Modifier
                    .size((size.value / 4.2f).dp)
                    .clip(CircleShape)
                    .background(MessengerColors.Success),
            )
        }
    }
}

fun formatChatListTime(epochMillis: Long?): String {
    if (epochMillis == null) return ""
    val zoned = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    return when (zoned.toLocalDate()) {
        today -> zoned.format(timeFormatter)
        today.minusDays(1) -> "Yesterday"
        else -> zoned.format(dateFormatter)
    }
}

fun formatMessageTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timeFormatter)

fun previewMessage(text: String?, fallback: String): String {
    val source = text?.trim().orEmpty().ifBlank { fallback }
    return if (source.length <= 40) source else source.take(37) + "..."
}

private fun initials(label: String): String =
    label.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.take(1) }
        .uppercase(Locale.getDefault())
        .ifBlank { "M" }

private fun colorFromLabel(label: String): Color {
    val palette = listOf(
        Color(0xFF6C5CE7),
        Color(0xFF00A8E8),
        Color(0xFFFF7675),
        Color(0xFF00B894),
        Color(0xFFF39C12),
    )
    return palette[(label.hashCode().ushr(1)) % palette.size]
}
