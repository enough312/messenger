package com.messenger.websocket

import com.messenger.shared.util.MessengerJson
import com.messenger.shared.ws.WsEnvelope
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.send
import java.util.concurrent.ConcurrentHashMap

class ConnectionManager {
    private val connections = ConcurrentHashMap<String, MutableSet<DefaultWebSocketSession>>()

    fun addConnection(userId: String, session: DefaultWebSocketSession) {
        connections.computeIfAbsent(userId) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun removeConnection(userId: String, session: DefaultWebSocketSession) {
        connections[userId]?.remove(session)
        if (connections[userId].isNullOrEmpty()) {
            connections.remove(userId)
        }
    }

    suspend fun sendToUser(userId: String, event: WsEnvelope) {
        connections[userId]?.toList()?.forEach { session: DefaultWebSocketSession ->
            runCatching {
                session.send(Frame.Text(MessengerJson.encodeToString(WsEnvelope.serializer(), event)))
            }.onFailure {
                removeConnection(userId, session)
            }
        }
    }

    suspend fun broadcast(userIds: List<String>, event: WsEnvelope) {
        userIds.distinct().forEach { userId -> sendToUser(userId, event) }
    }

    fun totalConnections(): Int = connections.values.sumOf { it.size }

    fun allUserIds(): List<String> = connections.keys.toList()
}
