package com.messenger.plugins

import com.messenger.config.AppGraph
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity(graph: AppGraph) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = graph.config.jwtRealm
            verifier(graph.authService.verifier())
            validate { credential ->
                val subject = credential.payload.subject
                if (subject.isNullOrBlank()) null else io.ktor.server.auth.jwt.JWTPrincipal(credential.payload)
            }
        }
    }
}
