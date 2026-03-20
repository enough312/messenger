package com.messenger.desktop.ui

import java.awt.Desktop
import java.io.File
import javax.swing.JFileChooser

fun pickAttachmentFile(): File? =
    runCatching {
        JFileChooser().apply {
            dialogTitle = "Choose a file"
            isMultiSelectionEnabled = false
        }.let { chooser ->
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
        }
    }.getOrNull()

fun openExternalUrl(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(java.net.URI(url))
        }
    }
}
