package com.messenger.plugins

import io.ktor.server.application.Application

fun Application.configureRateLimit() {
    // Reserved for redis-backed throttling. The route layer is ready for integration,
    // but we keep the plugin registration lightweight to avoid blocking local startup.
}
