package com.messenger.desktop.data

import com.messenger.shared.util.MessengerJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

class DesktopClient {
    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(MessengerJson)
        }
    }
}
