package com.messenger.routes

import com.messenger.config.AppGraph
import com.messenger.shared.dto.SignalBundleRequest
import com.messenger.shared.dto.SignalBundleResponse
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.signalRoutes(graph: AppGraph) {
    route("/signal") {
        post("/me") {
            val request = call.receive<SignalBundleRequest>()
            graph.signalKeyService.upload(call.currentUserId(), request.identityKey, request.signedPreKey, request.oneTimePreKeys)
            call.respond(mapOf("success" to true))
        }
        get("/{userId}") {
            val bundle = graph.signalKeyService.getBundle(call.parameters["userId"] ?: error("Missing user id"))
            call.respond(
                SignalBundleResponse(
                    identityKey = bundle.getValue("identityKey"),
                    signedPreKey = bundle.getValue("signedPreKey"),
                    oneTimePreKeys = bundle.getValue("oneTimePreKeys"),
                ),
            )
        }
    }
}
