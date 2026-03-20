package com.messenger.shared.ws

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

@Serializable
data class WsEnvelope(
    val type: String,
    val requestId: String? = null,
    val payload: JsonObject = JsonObject(emptyMap()),
)

object WsTypes {
    const val AUTH = "auth"
    const val AUTH_OK = "auth_ok"
    const val PING = "ping"
    const val PONG = "pong"
    const val SEND_MESSAGE = "send_message"
    const val NEW_MESSAGE = "new_message"
    const val MESSAGE_EDITED = "message_edited"
    const val MESSAGE_DELETED = "message_deleted"
    const val READ_MESSAGE = "read_message"
    const val TYPING_START = "typing_start"
    const val TYPING_STOP = "typing_stop"
    const val TYPING = "typing"
    const val USER_STATUS = "user_status"
    const val CALL_START = "call_start"
    const val CALL_INCOMING = "call_incoming"
    const val CALL_ANSWER = "call_answer"
    const val CALL_SDP = "call_sdp"
    const val CALL_ICE = "call_ice"
    const val CALL_END = "call_end"
    const val CALL_ENDED = "call_ended"
    const val REACTION_ADD = "reaction_add"
    const val REACTION_ADDED = "reaction_added"
    const val ERROR = "error"
}

inline fun <reified T> wsEnvelope(
    type: String,
    payload: T,
    json: Json = Json,
    requestId: String? = null,
): WsEnvelope = WsEnvelope(type = type, requestId = requestId, payload = json.encodeToJsonElement(payload).jsonObject)
