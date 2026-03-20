package com.messenger.routes

import com.messenger.config.AppGraph
import com.messenger.shared.dto.ContactRequest
import com.messenger.shared.dto.UpdateProfileRequest
import com.messenger.shared.dto.UserSearchResponse
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.userRoutes(graph: AppGraph) {
    route("/users") {
        get("/me") {
            call.respond(graph.userService.me(call.currentUserId()))
        }
        patch("/me") {
            call.respond(graph.userService.updateProfile(call.currentUserId(), call.receive<UpdateProfileRequest>()))
        }
        delete("/me") {
            graph.userService.deleteAccount(call.currentUserId())
            call.respond(mapOf("success" to true))
        }
        get("/search") {
            call.respond(UserSearchResponse(graph.userService.search(call.request.queryParameters["q"].orEmpty())))
        }
        get("/{id}") {
            call.respond(graph.userService.getById(call.parameters["id"] ?: error("Missing user id")))
        }
    }

    route("/contacts") {
        get {
            call.respond(graph.userService.contacts(call.currentUserId()))
        }
        post {
            val request = call.receive<ContactRequest>()
            graph.userService.addContact(call.currentUserId(), request.userId)
            call.respond(mapOf("success" to true))
        }
        delete("/{id}") {
            graph.userService.removeContact(call.currentUserId(), call.parameters["id"] ?: error("Missing contact id"))
            call.respond(mapOf("success" to true))
        }
        post("/{id}/block") {
            graph.userService.block(call.currentUserId(), call.parameters["id"] ?: error("Missing user id"))
            call.respond(mapOf("success" to true))
        }
        delete("/{id}/block") {
            graph.userService.unblock(call.currentUserId(), call.parameters["id"] ?: error("Missing user id"))
            call.respond(mapOf("success" to true))
        }
    }
}
