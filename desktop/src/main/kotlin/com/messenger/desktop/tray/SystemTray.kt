package com.messenger.desktop.tray

import java.awt.Image
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage

object SystemTrayController {
    private val trayIcon: TrayIcon? by lazy {
        if (!SystemTray.isSupported()) return@lazy null
        runCatching {
            val image = createTrayImage()
            TrayIcon(image, "Messenger").apply {
                isImageAutoSize = true
            }.also { SystemTray.getSystemTray().add(it) }
        }.getOrNull()
    }

    fun showNotification(title: String, message: String) {
        trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    private fun createTrayImage(): Image {
        return BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    }
}
