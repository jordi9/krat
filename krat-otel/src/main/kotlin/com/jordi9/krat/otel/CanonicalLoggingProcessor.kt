package com.jordi9.krat.otel

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.github.benmanes.caffeine.cache.Ticker
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.semconv.HttpAttributes
import org.slf4j.event.Level
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Emits one canonical log line per HTTP request (SERVER span) or background job (CONSUMER span).
 * Traces with neither are flushed as orphans on [orphanTimeout] timeout or [forceFlush].
 */
class CanonicalLoggingProcessor(
  format: LogFormat = LogFormat.PRETTY,
  orphanTimeout: Duration = Duration.ofMinutes(5),
  ticker: Ticker = Ticker.systemTicker()
) : SpanProcessor {

  private val logFormatter: LogFormatter? = when (format) {
    LogFormat.NONE -> null
    LogFormat.PRETTY -> PrettyFormatter()
    LogFormat.LOGFMT -> LogfmtFormatter()
  }

  private val traces = Caffeine
    .newBuilder()
    .ticker(ticker)
    .expireAfterWrite(orphanTimeout)
    .evictionListener<TraceId, MutableList<SpanData>> { traceId, spans, cause ->
      if (cause == RemovalCause.EXPIRED && traceId != null && spans != null) {
        logFormatter?.logOrphan(spans, traceId)
      }
    }
    .build<TraceId, MutableList<SpanData>>()

  override fun onEnd(span: ReadableSpan) {
    if (logFormatter == null) return

    val data = span.toSpanData()
    val traceId = TraceId(data.spanContext.traceId)

    traces.asMap().compute(traceId) { _, existing ->
      (existing ?: CopyOnWriteArrayList()).also { it.add(data) }
    }

    if (span.kind == SpanKind.SERVER || span.kind == SpanKind.CONSUMER) {
      val collected = traces.asMap().remove(traceId) ?: return
      val children = collected
        .filter { it.spanContext.spanId != data.spanContext.spanId }
        .sortedBy { it.startEpochNanos }
      logFormatter.log(data.toLevel(), data, children, traceId)
    }
  }

  override fun isStartRequired() = false

  override fun isEndRequired() = true

  override fun onStart(parent: Context, span: ReadWriteSpan) {}

  override fun forceFlush(): CompletableResultCode {
    if (logFormatter == null) return CompletableResultCode.ofSuccess()

    traces.cleanUp()

    // Snapshot keys, then atomically remove-and-emit each entry via compute so that
    // a concurrent onEnd adding spans to the same trace either lands before our compute
    // (gets included) or creates a fresh entry afterward (left for the next flush).
    val tracesSnapshot = traces.asMap()
    for (traceId in tracesSnapshot.keys.toList()) {
      tracesSnapshot.compute(traceId) { _, spans ->
        if (spans != null) logFormatter.logOrphan(spans, traceId)
        null
      }
    }
    return CompletableResultCode.ofSuccess()
  }
}

private fun SpanData.toLevel(): Level = when (this.kind) {
  SpanKind.SERVER -> {
    val status = this.attributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)?.toInt()
    when {
      status == null -> Level.WARN
      status >= 500 -> Level.ERROR
      status >= 400 -> Level.WARN
      else -> Level.INFO
    }
  }

  SpanKind.CONSUMER -> if (this.status.statusCode == StatusCode.ERROR) Level.ERROR else Level.INFO

  else -> Level.INFO
}
