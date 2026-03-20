package com.messenger.plugins

import com.messenger.config.AppGraph
import com.messenger.routes.authRoutes
import com.messenger.routes.chatRoutes
import com.messenger.routes.mediaUploadRoutes
import com.messenger.routes.messageRoutes
import com.messenger.routes.publicMediaRoutes
import com.messenger.routes.signalRoutes
import com.messenger.routes.userRoutes
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import java.lang.management.ManagementFactory
import kotlinx.serialization.Serializable

@Serializable
private data class HealthResponse(
    val status: String,
    val db: Boolean,
    val redis: Boolean,
    val uptime: Long,
    val version: String,
)

fun Application.configureRouting(graph: AppGraph) {
    routing {
        get("/health") {
            val dbOk = runCatching { graph.dataSource.connection.use { !it.isClosed } }.getOrDefault(false)
            val redisOk = runCatching { graph.redisClient.connect().use { it.sync().ping() == "PONG" } }.getOrDefault(false)
            call.respond(
                if (dbOk && redisOk) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable,
                HealthResponse(
                    status = if (dbOk && redisOk) "ok" else "degraded",
                    db = dbOk,
                    redis = redisOk,
                    uptime = ManagementFactory.getRuntimeMXBean().uptime / 1000,
                    version = "1.0.0",
                ),
            )
        }

        get("/metrics") {
            call.respondText(graph.registry.scrape(), ContentType.parse("text/plain; version=0.0.4"))
        }

        route("/auth") {
            authRoutes(graph)
        }

        publicMediaRoutes(graph)

        authenticate("auth-jwt") {
            userRoutes(graph)
            chatRoutes(graph)
            messageRoutes(graph)
            mediaUploadRoutes(graph)
            signalRoutes(graph)
        }
    }
}
