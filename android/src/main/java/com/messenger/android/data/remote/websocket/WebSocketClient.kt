package com.messenger.android.data.remote.websocket

import com.messenger.shared.util.MessengerJson
import com.messenger.shared.ws.WsEnvelope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
) {
    private val _events = MutableSharedFlow<WsEnvelope>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    private var socket: WebSocket? = null

    fun connect(baseUrl: String, accessToken: String) {
        val wsUrl = baseUrl.replace("http", "ws").trimEnd('/') + "/ws"
        socket = okHttpClient.newWebSocket(
            Request.Builder().url(wsUrl).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(MessengerJson.encodeToString(WsEnvelope.serializer(), WsEnvelope("auth", payload = buildJsonObject {
                        put("token", accessToken)
                    })))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    _events.tryEmit(MessengerJson.decodeFromString(WsEnvelope.serializer(), text))
                }
            },
        )
    }

    fun send(envelope: WsEnvelope) {
        socket?.send(MessengerJson.encodeToString(WsEnvelope.serializer(), envelope))
    }
}
