package com.jordi9.krat.pack.otel

import com.jordi9.krat.otel.withStartupTracer
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer

inline fun Application.startupTracer(openTelemetry: OpenTelemetry, block: (Tracer) -> Unit) {
  withStartupTracer(openTelemetry, block)
    .also { startup ->
      monitor.subscribe(ApplicationStarted) { startup.close() }
    }
}
