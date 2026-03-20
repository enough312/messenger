package com.messenger.plugins

import com.messenger.config.AppGraph
import com.messenger.shared.dto.CallAnswer
import com.messenger.shared.dto.CallOffer
import com.messenger.shared.dto.IceCandidate
import com.messenger.shared.dto.ReadMessageRequest
import com.messenger.shared.dto.ReactionRequest
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.util.MessengerJson
import com.messenger.shared.ws.WsEnvelope
import com.messenger.shared.ws.WsTypes
import com.messenger.shared.ws.wsEnvelope
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import io.ktor.server.websocket.webSocket
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun Application.configureWebSockets(graph: AppGraph) {
    install(WebSockets)

    routing {
        webSocket("/ws") {
            var userId: String? = null
            try {
                for (frame in incoming) {
                    if (frame !is Frame.Text) continue
                    val envelope = MessengerJson.decodeFromString(WsEnvelope.serializer(), frame.readText())
                    when (envelope.type) {
                        WsTypes.AUTH -> {
                            val token = envelope.payload["token"]?.toString()?.trim('"')
                            if (token.isNullOrBlank()) {
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Missing token"))
                                break
                            }
                            val principal = runCatching { graph.authService.verifier().verify(token) }.getOrNull()
                            userId = principal?.subject
                            if (userId == null) {
                                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))
                                break
                            }
                            graph.connectionManager.addConnection(userId!!, this)
                            graph.userService.setOnline(userId!!)
                            sendSerialized(wsEnvelope(WsTypes.AUTH_OK, buildJsonObject { put("userId", userId!!) }))
                        }

                        WsTypes.PING -> sendSerialized(wsEnvelope(WsTypes.PONG, buildJsonObject { }))

                        WsTypes.SEND_MESSAGE -> {
                            val uid = userId ?: continue
                            val request = MessengerJson.decodeFromJsonElement(SendMessageRequest.serializer(), envelope.payload)
                            val chatId = envelope.payload["chatId"]?.toString()?.trim('"') ?: continue
                            val message = graph.messageService.send(chatId, uid, request)
                            val recipients = graph.chatService.memberIds(chatId)
                            graph.connectionManager.broadcast(recipients, wsEnvelope(WsTypes.NEW_MESSAGE, message, requestId = envelope.requestId))
                        }

                        WsTypes.TYPING_START, WsTypes.TYPING_STOP -> {
                            val uid = userId ?: continue
                            val chatId = envelope.payload["chatId"]?.toString()?.trim('"') ?: continue
                            val recipients = graph.chatService.memberIds(chatId).filterNot { it == uid }
                            graph.connectionManager.broadcast(
                                recipients,
                                WsEnvelope(
                                    type = WsTypes.TYPING,
                                    requestId = envelope.requestId,
                                    payload = buildJsonObject {
                                        put("chatId", chatId)
                                        put("userId", uid)
                                        put("action", if (envelope.type == WsTypes.TYPING_START) "start" else "stop")
                                    },
                                ),
                            )
                        }

                        WsTypes.READ_MESSAGE -> {
                            val uid = userId ?: continue
                            val request = MessengerJson.decodeFromJsonElement(ReadMessageRequest.serializer(), envelope.payload)
                            graph.messageService.read(request.messageId, uid)
                        }

                        WsTypes.REACTION_ADD -> {
                            val uid = userId ?: continue
                            val messageId = envelope.payload["messageId"]?.toString()?.trim('"') ?: continue
                            val request = MessengerJson.decodeFromJsonElement(ReactionRequest.serializer(), envelope.payload)
                            val message = graph.messageService.react(messageId, uid, request.emoji)
                            val recipients = graph.chatService.memberIds(message.chatId)
                            graph.connectionManager.broadcast(recipients, wsEnvelope(WsTypes.REACTION_ADDED, buildJsonObject {
                                put("messageId", messageId)
                                put("emoji", request.emoji)
                                put("userId", uid)
                            }))
                        }

                        WsTypes.CALL_START -> {
                            val uid = userId ?: continue
                            val request = MessengerJson.decodeFromJsonElement(CallOffer.serializer(), envelope.payload)
                            val callId = graph.callService.start(request.chatId, uid, request.callType)
                            val recipients = graph.chatService.memberIds(request.chatId).filterNot { it == uid }
                            graph.connectionManager.broadcast(recipients, WsEnvelope(
                                type = WsTypes.CALL_INCOMING,
                                payload = buildJsonObject {
                                    put("callId", callId)
                                    put("callerId", uid)
                                    put("type", request.callType)
                                },
                            ))
                        }

                        WsTypes.CALL_ANSWER -> {
                            val answer = MessengerJson.decodeFromJsonElement(CallAnswer.serializer(), envelope.payload)
                            graph.connectionManager.broadcast(graph.connectionManager.allUserIds(), wsEnvelope(WsTypes.CALL_SDP, answer))
                        }

                        WsTypes.CALL_ICE -> {
                            val candidate = MessengerJson.decodeFromJsonElement(IceCandidate.serializer(), envelope.payload)
                            graph.connectionManager.broadcast(graph.connectionManager.allUserIds(), wsEnvelope(WsTypes.CALL_ICE, candidate))
                        }

                        WsTypes.CALL_END -> {
                            val callId = envelope.payload["callId"]?.toString()?.trim('"') ?: continue
                            graph.callService.end(callId)
                            graph.connectionManager.broadcast(graph.connectionManager.allUserIds(), WsEnvelope(
                                type = WsTypes.CALL_ENDED,
                                payload = buildJsonObject {
                                    put("callId", callId)
                                    put("reason", "hangup")
                                },
                            ))
                        }
                    }
                }
            } finally {
                userId?.let {
                    graph.connectionManager.removeConnection(it, this)
                    graph.userService.setOffline(it)
                }
            }
        }
    }
}

private suspend fun DefaultWebSocketSession.sendSerialized(envelope: WsEnvelope) {
    send(Frame.Text(MessengerJson.encodeToString(WsEnvelope.serializer(), envelope)))
}
