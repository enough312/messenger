package com.messenger.desktop

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.messenger.desktop.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    val koin = startKoin { modules(appModule) }.koin
    Window(
        onCloseRequest = ::exitApplication,
        title = "Messenger",
    ) {
        MaterialTheme {
            App(koin)
        }
    }
}

@Composable
fun App(koin: org.koin.core.Koin) {
    com.messenger.desktop.ui.MainWindow(koin)
}
