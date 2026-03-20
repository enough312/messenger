package com.messenger.routes

import com.messenger.shared.util.MessengerJson
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

fun ApplicationCall.currentUserId(): String = principal<JWTPrincipal>()?.payload?.subject ?: error("Missing auth principal")
