package com.jordi9.krat.otel

import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.semconv.ExceptionAttributes
import io.opentelemetry.semconv.HttpAttributes
import org.slf4j.event.Level

internal interface LogFormatter {
  fun log(level: Level, rootSpan: SpanData, children: List<SpanData>, traceId: TraceId)
  fun logOrphan(spans: List<SpanData>, traceId: TraceId)
}

const val CANONICAL_LOGGER = "CanonicalTrace"

internal data class ExceptionInfo(
  val type: String,
  val message: String,
  val stacktrace: String?
)

internal fun SpanData.toExceptionInfo(): ExceptionInfo? {
  val event = events.find { it.name == "exception" } ?: return null
  return ExceptionInfo(
    type = event.attributes.get(ExceptionAttributes.EXCEPTION_TYPE) ?: "Unknown",
    message = event.attributes.get(ExceptionAttributes.EXCEPTION_MESSAGE) ?: "",
    stacktrace = event.attributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE)
  )
}

@JvmInline
internal value class TraceId(val value: String)

internal val SpanData.durationMs: Long
  get() = (endEpochNanos - startEpochNanos) / 1_000_000

// Ktor's CORS plugin returns 403 without setting http.route when origin is not allowed
// and only logs reasons at TRACE (KTOR-4236), so we hint "CORS?" to aid debugging.
internal val SpanData.isLikelyCors: Boolean
  get() = attributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)?.toInt() == 403 &&
    attributes.get(HttpAttributes.HTTP_ROUTE) == null
