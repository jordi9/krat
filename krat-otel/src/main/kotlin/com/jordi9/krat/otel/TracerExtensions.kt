package com.jordi9.krat.otel

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext

/**
 * Execute a block of code within a traced span, handling lifecycle and error recording automatically.
 *
 * Usage:
 * ```kotlin
 * tracer.withSpan("operation.name", { setAttribute("key", value) }) {
 *   // your code here
 *   setAttribute("result.count", items.size.toLong())
 *   items
 * }
 * ```
 */
inline fun <T> Tracer.withSpan(spanName: String, configure: SpanBuilder.() -> Unit = {}, block: Span.() -> T): T {
  val span = spanBuilder(spanName).apply(configure).startSpan()
  return try {
    span.makeCurrent().use {
      span.block()
    }
  } catch (e: Exception) {
    span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
    span.recordException(e)
    throw e
  } finally {
    span.end()
  }
}

/**
 * Execute a suspending block of code within a CONSUMER span for background workers,
 * with proper coroutine context propagation.
 */
suspend fun <T> Tracer.withWorkerSpan(
  spanName: String,
  configure: SpanBuilder.() -> Unit = {},
  block: suspend Span.() -> T
): T {
  val span =
    spanBuilder(spanName)
      .setSpanKind(SpanKind.CONSUMER)
      .apply(configure)
      .startSpan()
  return try {
    withContext(span.asContextElement()) {
      span.makeCurrent().use {
        span.block()
      }
    }
  } catch (e: Exception) {
    span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
    span.recordException(e)
    throw e
  } finally {
    span.end()
  }
}
