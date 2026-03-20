package com.messenger.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.messenger.desktop.state.DesktopAppState
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.ui.panels.ChatPanel
import com.messenger.desktop.ui.panels.InfoPanel
import com.messenger.desktop.ui.panels.SidebarPanel
import org.koin.core.Koin

@Composable
fun MainWindow(koin: Koin) {
    val state = remember { koin.get<DesktopAppState>() }
    if (state.accessToken == null) {
        AuthScreen(state)
        return
    }

    Surface(color = MessengerColors.SurfaceSoft) {
        Row(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(
                modifier = Modifier
                    .width(340.dp)
                    .fillMaxHeight()
                    .background(MessengerColors.Sidebar),
            ) {
                SidebarPanel(
                    state = state,
                    onSelectChat = state::loadMessages,
                )
            }
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MessengerColors.Border)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                if (state.selectedChatId != null) {
                    ChatPanel(state, state.selectedChatId!!)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MessengerColors.SurfaceSoft),
                    ) {
                        Text(
                            "Pick a chat or start a new one from search",
                            modifier = Modifier.padding(24.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = MessengerColors.Border)
            InfoPanel(state)
        }
    }
}
