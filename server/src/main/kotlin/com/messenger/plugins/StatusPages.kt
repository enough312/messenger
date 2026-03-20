package com.messenger.plugins

import com.messenger.model.ApiError
import com.messenger.service.ServiceException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureStatusPages() {
    val app = this
    install(StatusPages) {
        exception<ServiceException> { call, cause ->
            call.respond(HttpStatusCode.fromValue(cause.statusCode), ApiError(cause.message))
        }
        exception<Throwable> { call, cause ->
            app.environment.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ApiError(cause.message ?: "Internal server error"))
        }
    }
}
