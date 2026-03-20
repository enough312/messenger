package com.messenger.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import java.io.File
import java.io.FileInputStream

class PushService(credentialsPath: String?) {
    private val messaging: FirebaseMessaging?

    init {
        messaging = credentialsPath
            ?.let(::File)
            ?.takeIf { it.exists() }
            ?.let { file ->
                if (FirebaseApp.getApps().isEmpty()) {
                    val options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(FileInputStream(file)))
                        .build()
                    FirebaseApp.initializeApp(options)
                }
                FirebaseMessaging.getInstance()
            }
    }

    fun send(token: String, title: String, body: String) {
        val firebase = messaging ?: return
        val message = Message.builder()
            .setToken(token)
            .putData("title", title)
            .putData("body", body)
            .build()
        firebase.sendAsync(message)
    }
}
