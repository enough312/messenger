package com.messenger.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.messenger.desktop.ui.MessengerColors
import com.messenger.desktop.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    val koin = startKoin { modules(appModule) }.koin
    Window(
        onCloseRequest = ::exitApplication,
        title = "Messenger",
    ) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = MessengerColors.Accent,
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = MessengerColors.AccentSoft,
                secondaryContainer = MessengerColors.BubbleOther,
                surface = androidx.compose.ui.graphics.Color.White,
                surfaceVariant = MessengerColors.Sidebar,
                background = MessengerColors.SurfaceSoft,
                onSurface = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
                outlineVariant = MessengerColors.Border,
            ),
        ) {
            App(koin)
        }
    }
}

@Composable
fun App(koin: org.koin.core.Koin) {
    com.messenger.desktop.ui.MainWindow(koin)
}
