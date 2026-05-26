package com.jordi9.krat.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer

inline fun withStartupTracer(openTelemetry: OpenTelemetry, block: Tracer.() -> Unit): AutoCloseable {
  val tracer = openTelemetry.getTracer("krat-otel")

  val span = tracer
    .spanBuilder("app-start")
    .setSpanKind(SpanKind.INTERNAL)
    .startSpan()

  try {
    span.makeCurrent().use {
      block(tracer)
    }
  } catch (e: Throwable) {
    span.recordError(e)
    span.end()
    throw e
  }

  return AutoCloseable { span.end() }
}
