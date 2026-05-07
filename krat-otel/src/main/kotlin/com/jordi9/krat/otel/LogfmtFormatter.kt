package com.jordi9.krat.otel

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.sdk.trace.data.SpanData
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level

internal class LogfmtFormatter : LogFormatter {

  private val logger = LoggerFactory.getLogger(CANONICAL_LOGGER)

  override fun log(level: Level, rootSpan: SpanData, children: List<SpanData>, traceId: TraceId) {
    withMdc {
      rootSpan.attributes.asMap().forEach { (k, v) ->
        if (EXCLUDED_PREFIXES.none { k.key.startsWith(it) }) MDC.put(k.key, v.toString().singleLine())
      }
      MDC.put("name", rootSpan.name)
      MDC.put("trace_id", traceId.value)
      MDC.put("duration_ms", rootSpan.durationMs.toString())

      if (rootSpan.kind == SpanKind.CONSUMER) {
        val isError = rootSpan.status.statusCode == StatusCode.ERROR
        MDC.put("outcome", if (isError) "ERROR" else "OK")
      }

      if (rootSpan.kind == SpanKind.SERVER && rootSpan.isLikelyCors) {
        MDC.put("hint", "cors")
      }

      children.addToMdc()

      rootSpan.toExceptionInfo()?.let { ex ->
        MDC.put("exception.type", ex.type)
        MDC.put("exception.message", ex.message.singleLine())
        ex.stacktrace?.let { MDC.put("exception.stacktrace", it.singleLine()) }
      }

      logger.atLevel(level).log("")
    }
  }

  override fun logOrphan(spans: List<SpanData>, traceId: TraceId) {
    if (spans.isEmpty()) return
    val sorted = spans.sortedBy { it.startEpochNanos }
    withMdc {
      MDC.put("event", "trace_orphaned")
      MDC.put("trace_id", traceId.value)
      MDC.put("span_count", sorted.size.toString())
      sorted.addToMdc()
      logger.warn("")
    }
  }
}

private fun List<SpanData>.addToMdc() {
  forEachIndexed { i, span ->
    MDC.put("span.$i.name", span.name)
    MDC.put("span.$i.duration_ms", span.durationMs.toString())
    span.attributes.asMap().forEach { (k, v) ->
      if (EXCLUDED_PREFIXES.none { k.key.startsWith(it) }) {
        MDC.put("span.$i.${k.key}", v.toString().singleLine())
      }
    }
  }
}

private fun String.singleLine(): String = lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

private val EXCLUDED_PREFIXES = setOf("network.", "user_agent.", "server.")

private inline fun withMdc(block: () -> Unit) {
  val before = MDC.getCopyOfContextMap()
  try {
    block()
  } finally {
    MDC.clear()
    if (before != null) MDC.setContextMap(before)
  }
}
