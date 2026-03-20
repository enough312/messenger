package com.messenger.routes

import com.messenger.config.AppGraph
import com.messenger.shared.dto.AddChatMemberRequest
import com.messenger.shared.dto.ChatListResponse
import com.messenger.shared.dto.CreateChannelRequest
import com.messenger.shared.dto.CreateGroupChatRequest
import com.messenger.shared.dto.CreatePrivateChatRequest
import com.messenger.shared.dto.UpdateChatMemberRoleRequest
import com.messenger.shared.dto.UpdateChatRequest
import com.messenger.shared.dto.SendMessageRequest
import com.messenger.shared.dto.MessageListResponse
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.chatRoutes(graph: AppGraph) {
    route("/chats") {
        get {
            call.respond(ChatListResponse(graph.chatService.list(call.currentUserId())))
        }
        post("/private") {
            call.respond(graph.chatService.createPrivate(call.currentUserId(), call.receive<CreatePrivateChatRequest>()))
        }
        post("/group") {
            call.respond(graph.chatService.createGroup(call.currentUserId(), call.receive<CreateGroupChatRequest>()))
        }
        post("/channel") {
            call.respond(graph.chatService.createChannel(call.currentUserId(), call.receive<CreateChannelRequest>()))
        }
        get("/{id}") {
            call.respond(graph.chatService.get(call.parameters["id"] ?: error("Missing chat id"), call.currentUserId()))
        }
        patch("/{id}") {
            call.respond(graph.chatService.update(call.parameters["id"] ?: error("Missing chat id"), call.receive<UpdateChatRequest>()))
        }
        delete("/{id}") {
            graph.chatService.deleteOrLeave(call.parameters["id"] ?: error("Missing chat id"), call.currentUserId())
            call.respond(mapOf("success" to true))
        }
        get("/{id}/members") {
            call.respond(graph.chatService.members(call.parameters["id"] ?: error("Missing chat id")))
        }
        post("/{id}/members") {
            graph.chatService.addMember(call.parameters["id"] ?: error("Missing chat id"), call.receive<AddChatMemberRequest>())
            call.respond(mapOf("success" to true))
        }
        delete("/{id}/members/{uid}") {
            graph.chatService.removeMember(
                call.parameters["id"] ?: error("Missing chat id"),
                call.parameters["uid"] ?: error("Missing member id"),
            )
            call.respond(mapOf("success" to true))
        }
        patch("/{id}/members/{uid}/role") {
            graph.chatService.updateMemberRole(
                call.parameters["id"] ?: error("Missing chat id"),
                call.parameters["uid"] ?: error("Missing member id"),
                call.receive<UpdateChatMemberRoleRequest>(),
            )
            call.respond(mapOf("success" to true))
        }
        get("/{id}/messages") {
            val chatId = call.parameters["id"] ?: error("Missing chat id")
            val cursor = call.request.queryParameters["cursor"]
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
            val items = graph.messageService.list(chatId, call.currentUserId(), cursor, limit)
            call.respond(MessageListResponse(items, items.lastOrNull()?.createdAt?.toString()))
        }
        post("/{id}/messages") {
            val chatId = call.parameters["id"] ?: error("Missing chat id")
            val message = graph.messageService.send(chatId, call.currentUserId(), call.receive<SendMessageRequest>())
            graph.connectionManager.broadcast(graph.chatService.memberIds(chatId), com.messenger.shared.ws.wsEnvelope(com.messenger.shared.ws.WsTypes.NEW_MESSAGE, message))
            call.respond(message)
        }
    }
}
