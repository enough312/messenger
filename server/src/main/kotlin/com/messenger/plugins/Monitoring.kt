package com.messenger.plugins

import com.messenger.config.AppGraph
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.metrics.micrometer.MicrometerMetrics
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.forwardedheaders.ForwardedHeaders
import org.slf4j.event.Level

fun Application.configureMonitoring(graph: AppGraph) {
    install(ForwardedHeaders)
    install(CallLogging) {
        level = Level.INFO
    }
    install(MicrometerMetrics) {
        registry = graph.registry
    }
}
