package com.messenger.routes

import com.messenger.config.AppGraph
import com.messenger.shared.dto.ReactionRequest
import com.messenger.shared.dto.UpdateMessageRequest
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.messageRoutes(graph: AppGraph) {
    route("/messages") {
        patch("/{id}") {
            call.respond(graph.messageService.update(call.parameters["id"] ?: error("Missing message id"), call.receive<UpdateMessageRequest>()))
        }
        delete("/{id}") {
            graph.messageService.delete(call.parameters["id"] ?: error("Missing message id"))
            call.respond(mapOf("success" to true))
        }
        post("/{id}/pin") {
            call.respond(graph.messageService.pin(call.parameters["id"] ?: error("Missing message id"), pinned = true))
        }
        delete("/{id}/pin") {
            call.respond(graph.messageService.pin(call.parameters["id"] ?: error("Missing message id"), pinned = false))
        }
        post("/{id}/reactions") {
            val messageId = call.parameters["id"] ?: error("Missing message id")
            call.respond(graph.messageService.react(messageId, call.currentUserId(), call.receive<ReactionRequest>().emoji))
        }
    }
}
