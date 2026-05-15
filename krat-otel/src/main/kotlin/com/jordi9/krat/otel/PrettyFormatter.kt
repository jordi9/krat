package com.jordi9.krat.otel

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

internal class PrettyFormatter : LogFormatter {

  private val logger = LoggerFactory.getLogger(CANONICAL_LOGGER)

  override fun log(level: Level, rootSpan: SpanData, children: List<SpanData>, traceId: TraceId) {
    logger.atLevel(level).log(render(rootSpan, children, traceId, level))
  }

  override fun logOrphan(spans: List<SpanData>, traceId: TraceId) {
    if (spans.isEmpty()) return
    val sorted = spans.sortedBy { it.startEpochNanos }
    logger.warn(renderOrphan(sorted, traceId))
  }
}

private fun render(rootSpan: SpanData, children: List<SpanData>, traceId: TraceId, level: Level): String {
  val attrs = rootSpan.bodyAttributes().toPretty()
  return buildString {
    appendLine()
    appendLine("  ${header(rootSpan, traceId)}")
    if (attrs != null) appendLine("    $attrs")
    for (child in children) {
      val childAttrs = child.attributes.asMap().toPretty()?.replace("\n", "\n      ")
      appendLine("    ↳ ${child.name} ${child.durationMs}ms${if (childAttrs != null) " $childAttrs" else ""}")
    }
    rootSpan.toExceptionInfo()?.let { appendException(it, includeStacktrace = level == Level.ERROR) }
  }.trimEnd()
}

private fun header(rootSpan: SpanData, traceId: TraceId): String {
  val durationMs = rootSpan.durationMs
  return when (rootSpan.kind) {
    SpanKind.SERVER -> {
      val method = rootSpan.attributes.get(HttpAttributes.HTTP_REQUEST_METHOD) ?: "?"
      val displayRoute = rootSpan.attributes.get(HttpAttributes.HTTP_ROUTE)
        ?: rootSpan.attributes.get(UrlAttributes.URL_PATH)
        ?: rootSpan.name
      val status = rootSpan.attributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)?.toInt()
      val statusHint = if (rootSpan.isLikelyCors) "403 CORS?" else (status?.toString() ?: "?")
      "${serverSymbol(status)} [$method $displayRoute] ($statusHint) ${durationMs}ms | ${traceId.value}"
    }

    SpanKind.CONSUMER, SpanKind.INTERNAL -> {
      val isError = rootSpan.status.statusCode == StatusCode.ERROR
      val symbol = if (isError) "✗" else "✓"
      "$symbol [${rootSpan.name}] ${if (isError) "ERROR" else "OK"} ${durationMs}ms | ${traceId.value}"
    }

    else -> error("Unexpected SpanKind: ${rootSpan.kind}")
  }
}

private fun renderOrphan(spans: List<SpanData>, traceId: TraceId) = buildString {
  appendLine()
  appendLine("  ⏱ [orphan trace] ${spans.size} spans (no root) | ${traceId.value}")
  for (span in spans) appendLine("    ↳ ${span.name} ${span.durationMs}ms")
}.trimEnd()

private fun StringBuilder.appendException(e: ExceptionInfo, includeStacktrace: Boolean) {
  appendLine("    ${e.type}: ${e.message}")
  if (includeStacktrace) {
    e.stacktrace?.lines()?.drop(1)?.take(10)?.forEach { line ->
      appendLine("    $line")
    }
  }
}

private fun serverSymbol(status: Int?): String = when {
  status == null -> "?"
  status >= 500 -> "✗"
  status >= 400 -> "⚠"
  else -> "✓"
}

private val EXCLUDED_PREFIXES = setOf("url.", "server.", "network.", "user_agent.")

private val SERVER_HEADER_KEYS = setOf("http.request.method", "http.route", "http.response.status_code")

private fun SpanData.bodyAttributes(): Map<AttributeKey<*>, Any> {
  val notNoise = attributes.asMap().filterKeys { k -> EXCLUDED_PREFIXES.none { k.key.startsWith(it) } }
  return when (kind) {
    SpanKind.SERVER -> notNoise.filterKeys { it.key !in SERVER_HEADER_KEYS }
    else -> notNoise
  }
}

private fun Map<AttributeKey<*>, Any>.toPretty(): String? {
  if (isEmpty()) return null
  return entries
    .map { (k, v) -> "${k.key}=${quote(v.toString())}" }
    .sorted()
    .joinToString(" ")
}

private fun quote(value: String): String {
  val needsQuoting = value.contains(' ') || value.contains('=') || value.contains('"')
  if (!needsQuoting) return value
  return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
