package com.jordi9.krat.otel

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

private const val APP_ATTRIBUTE_PREFIX = "app."

/**
 * Helper class for setting app-namespaced attributes on a Span using map like syntax.
 *
 * Usage: `span.appAttribute["count"] = 42`
 */
class SpanAppAttributeSetter(private val span: Span) {
  operator fun set(key: String, value: String) {
    span.setAttribute("$APP_ATTRIBUTE_PREFIX$key", value)
  }

  operator fun set(key: String, value: Long) {
    span.setAttribute("$APP_ATTRIBUTE_PREFIX$key", value)
  }

  operator fun set(key: String, value: Int) {
    span.setAttribute("$APP_ATTRIBUTE_PREFIX$key", value.toLong())
  }

  operator fun set(key: String, value: Boolean) {
    span.setAttribute("$APP_ATTRIBUTE_PREFIX$key", value)
  }
}

/**
 * Access app-namespaced attributes using indexed syntax.
 *
 * Usage: `span.appAttribute["userId"] = "user-123"`
 */
val Span.appAttribute: SpanAppAttributeSetter get() = SpanAppAttributeSetter(this)

/**
 * Execute a block with the current span as a receiver.
 *
 * Syntactic sugar for grouping span operations. If no trace is active, `Span.current()`
 * returns a no-op span that safely discards all operations (OTel's intentional design).
 *
 * Usage:
 * ```
 * withCurrentSpan {
 *   appAttribute["userId"] = userId
 *   appAttribute["count"] = count
 * }
 * ```
 */
inline fun withCurrentSpan(block: Span.() -> Unit) {
  Span.current().block()
}

/**
 * Trace attributes from this value to the current span, then return the value.
 *
 * Similar to [also], but with the current [Span] as the block receiver.
 * If no trace is active, the block still executes on a no-op span (OTel's intentional design).
 *
 * Usage:
 * ```
 * fetchUser(id)
 *   .traced { user ->
 *     appAttribute["userId"] = user.id
 *     appAttribute["role"] = user.role
 *   }
 * ```
 */
inline fun <T> T.traced(block: Span.(T) -> Unit): T {
  Span.current().block(this)
  return this
}

/**
 * Record an error on this span by setting ERROR status and recording the exception.
 *
 * OTel's [Span.recordException] only adds an exception event - it doesn't set the span status.
 * This helper does both in one call.
 */
fun Span.recordError(e: Throwable): Span {
  setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
  recordException(e)
  return this
}
