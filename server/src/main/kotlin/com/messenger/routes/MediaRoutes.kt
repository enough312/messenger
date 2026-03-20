package com.messenger.routes

import com.messenger.config.AppGraph
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.publicMediaRoutes(graph: AppGraph) {
    route("/media") {
        get("/{objectKey}") {
            val objectKey = call.parameters["objectKey"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing object key"))
            val response = graph.mediaService.download(objectKey)
            call.respondBytes(response.bytes, ContentType.parse(response.contentType))
        }
    }
}

fun Route.mediaUploadRoutes(graph: AppGraph) {
    route("/media") {
        post("/upload") {
            val bytes = call.receive<ByteArray>()
            val contentType = call.request.headers["Content-Type"] ?: "application/octet-stream"
            val fileName = call.request.headers["X-File-Name"]
            call.respond(graph.mediaService.upload(contentType, bytes, fileName))
        }
    }
}
