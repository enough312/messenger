package com.messenger.routes

import com.messenger.config.AppGraph
import com.messenger.shared.dto.EnableTwoFactorRequest
import com.messenger.shared.dto.ForgotPasswordRequest
import com.messenger.shared.dto.LoginRequest
import com.messenger.shared.dto.LogoutRequest
import com.messenger.shared.dto.RefreshRequest
import com.messenger.shared.dto.RegisterRequest
import com.messenger.shared.dto.ResetPasswordRequest
import com.messenger.shared.dto.VerifyEmailRequest
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.request.userAgent
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post

fun Route.authRoutes(graph: AppGraph) {
    post("/register") {
        call.respond(graph.authService.register(call.receive<RegisterRequest>()))
    }
    post("/login") {
        val request = call.receive<LoginRequest>()
        val ip = call.request.local.remoteHost
        call.respond(graph.authService.login(request, ipAddress = ip, userAgent = call.request.userAgent()))
    }
    post("/refresh") {
        call.respond(graph.authService.refresh(call.receive<RefreshRequest>()))
    }
    post("/logout") {
        call.respond(graph.authService.logout(call.receive<LogoutRequest>()))
    }
    post("/verify-email") {
        call.respond(graph.authService.verifyEmail(call.receive<VerifyEmailRequest>()))
    }
    post("/forgot-password") {
        call.respond(graph.authService.forgotPassword(call.receive<ForgotPasswordRequest>()))
    }
    post("/reset-password") {
        call.respond(graph.authService.resetPassword(call.receive<ResetPasswordRequest>()))
    }

    authenticate("auth-jwt") {
        get("/2fa/setup") {
            call.respond(graph.authService.setupTwoFactor(call.currentUserId()))
        }
        post("/2fa/enable") {
            val request = call.receive<EnableTwoFactorRequest>()
            call.respond(graph.authService.enableTwoFactor(call.currentUserId(), request.code))
        }
    }
}
