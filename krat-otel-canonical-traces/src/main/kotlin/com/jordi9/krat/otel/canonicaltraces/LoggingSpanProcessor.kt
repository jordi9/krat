package com.jordi9.krat.otel.canonicaltraces

import com.github.benmanes.caffeine.cache.Caffeine
import com.jordi9.krat.otel.LogFormat
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.semconv.ExceptionAttributes
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * A SpanProcessor that logs trace summaries when SERVER or CONSUMER spans complete.
 */
class LoggingSpanProcessor(
  private val format: LogFormat = LogFormat.PRETTY,
  orphanTimeoutMinutes: Long = 5
) : SpanProcessor {

  private val logger = LoggerFactory.getLogger("TraceLog")

  private val traces = Caffeine
    .newBuilder()
    .expireAfterWrite(orphanTimeoutMinutes, TimeUnit.MINUTES)
    .build<TraceId, MutableList<SpanData>>()

  override fun onEnd(span: ReadableSpan) {
    val traceId = TraceId(span.spanContext.traceId)
    val spanData = span.toSpanData()

    traces.get(traceId) { mutableListOf() }.add(spanData)

    val info: SpanInfo? = when (span.kind) {
      SpanKind.SERVER -> spanData.toServerSpan(traceId)
      SpanKind.CONSUMER -> spanData.toWorkerSpan(traceId)
      else -> null
    }

    info?.let { logTraceSummary(it, spanData, traceId) }
  }

  private fun logTraceSummary(info: SpanInfo, rootSpan: SpanData, traceId: TraceId) {
    val spans = traces.asMap().remove(traceId) ?: return

    val message = when (format) {
      LogFormat.NONE -> return
      LogFormat.PRETTY -> formatPretty(info, rootSpan, spans)
      LogFormat.LOGFMT -> formatLogfmt(info, rootSpan, spans)
    }

    when (info.logLevel) {
      LogLevel.INFO -> logger.info(message)
      LogLevel.WARN -> logger.warn(message)
      LogLevel.ERROR -> logger.error(message)
    }
  }

  private fun formatLogfmt(info: SpanInfo, rootSpan: SpanData, spans: List<SpanData>) = buildString {
    append(info.logfmtHeader())
    appendSpaced(rootSpan.includedAttributes(info).toLogfmt())
    appendSpaced(formatChildSpansLogfmt(spans, rootSpan))
    rootSpan.extractException()?.let { append(" ${it.toLogfmt()}") }
  }

  private fun formatPretty(info: SpanInfo, rootSpan: SpanData, spans: List<SpanData>): String {
    val attrs = rootSpan.includedAttributes(info).toPrettyString()

    return buildString {
      appendLine()
      appendLine("  ${info.statusSymbol} ${info.prettyHeader()}")
      if (attrs.isNotEmpty()) appendLine("    $attrs")
      appendChildSpansPretty(spans, rootSpan)
      rootSpan.extractException()?.let { appendException(it, includeStacktrace = info.logLevel == LogLevel.ERROR) }
    }.trimEnd()
  }

  private fun formatChildSpansLogfmt(spans: List<SpanData>, rootSpan: SpanData): String =
    spans.childSpans(rootSpan).joinToString(SEPARATOR) { child ->
      "span.${child.name}=${child.durationMs}ms"
    }

  private fun StringBuilder.appendSpaced(value: String) {
    if (value.isNotEmpty()) append(" $value")
  }

  private fun StringBuilder.appendChildSpansPretty(spans: List<SpanData>, rootSpan: SpanData) {
    spans.childSpans(rootSpan).forEach { child ->
      val childAttrs =
        child.attributes
          .asMap()
          .toPrettyString()
          .replace("\n", "\n      ")
      appendLine(
        "    ↳ ${child.name} ${child.durationMs}ms${if (childAttrs.isNotEmpty()) " $childAttrs" else ""}"
      )
    }
  }

  private fun StringBuilder.appendException(exception: ExceptionInfo, includeStacktrace: Boolean) {
    appendLine("    ${exception.type}: ${exception.message}")
    if (includeStacktrace) {
      exception.stacktrace?.lines()?.drop(1)?.take(10)?.forEach { line ->
        appendLine("    $line")
      }
    }
  }

  private fun List<SpanData>.childSpans(rootSpan: SpanData) =
    filter { it.spanContext.spanId != rootSpan.spanContext.spanId }
      .sortedBy { it.startEpochNanos }

  override fun isStartRequired() = false

  override fun isEndRequired() = true

  override fun onStart(parent: Context, span: ReadWriteSpan) {}
}

private sealed class SpanInfo(val traceId: TraceId, val durationMs: Long) {
  abstract val logLevel: LogLevel
  abstract val statusSymbol: String
  abstract fun logfmtHeader(): String
  abstract fun prettyHeader(): String
  abstract fun includeAttribute(key: String): Boolean
}

private class ServerSpan(
  traceId: TraceId,
  durationMs: Long,
  val method: String,
  val route: String,
  val path: String?,
  val hasRoute: Boolean,
  val status: Int?
) : SpanInfo(traceId, durationMs) {

  // Ktor's CORS plugin returns 403 without setting http.route when origin is not allowed.
  // It logs reasons at TRACE level only (KTOR-4236), so we hint "CORS?" to aid debugging.
  // See: https://youtrack.jetbrains.com/issue/KTOR-4236
  private val likelyCors = status == 403 && !hasRoute

  override val logLevel: LogLevel = when {
    status == null -> LogLevel.WARN
    status >= 500 -> LogLevel.ERROR
    status >= 400 -> LogLevel.WARN
    else -> LogLevel.INFO
  }

  override val statusSymbol: String = when {
    status == null -> "?"
    status >= 500 -> "✗"
    status >= 400 -> "⚠"
    else -> "✓"
  }

  private val displayRoute = if (hasRoute) route else path ?: route
  private val statusHint = if (likelyCors) "403 CORS?" else (status?.toString() ?: "?")

  override fun logfmtHeader() = buildString {
    append("method=$method route=$displayRoute status=${status ?: "?"}")
    if (likelyCors) append(" hint=cors")
    append(" duration=${durationMs}ms trace_id=${traceId.value}")
  }

  override fun prettyHeader() = "[$method $displayRoute] ($statusHint) ${durationMs}ms | ${traceId.value}"

  override fun includeAttribute(key: String) = EXCLUDED_PREFIXES.none { key.startsWith(it) } && key !in EXCLUDED_KEYS

  companion object {
    private val EXCLUDED_PREFIXES = setOf("url.", "server.", "network.", "user_agent.")
    private val EXCLUDED_KEYS = setOf("http.request.method", "http.route", "http.response.status_code")
  }
}

private class WorkerSpan(
  traceId: TraceId,
  durationMs: Long,
  val name: String,
  val isError: Boolean
) : SpanInfo(traceId, durationMs) {

  override val logLevel = if (isError) LogLevel.ERROR else LogLevel.INFO
  override val statusSymbol = if (isError) "✗" else "✓"

  override fun logfmtHeader() = "worker=$name outcome=${if (isError) "ERROR" else "OK"} " +
    "duration=${durationMs}ms trace_id=${traceId.value}"

  override fun prettyHeader() = "[$name] ${if (isError) "ERROR" else "OK"} ${durationMs}ms | ${traceId.value}"

  override fun includeAttribute(key: String) = true
}

private enum class LogLevel { INFO, WARN, ERROR }

@JvmInline
private value class TraceId(
  val value: String
)

private val SpanData.durationMs: Long
  get() = (endEpochNanos - startEpochNanos) / 1_000_000

private fun SpanData.toServerSpan(traceId: TraceId): ServerSpan {
  val route = attributes.get(HttpAttributes.HTTP_ROUTE)
  return ServerSpan(
    traceId = traceId,
    durationMs = durationMs,
    method = attributes.get(HttpAttributes.HTTP_REQUEST_METHOD) ?: "?",
    route = route ?: name,
    path = attributes.get(UrlAttributes.URL_PATH),
    hasRoute = route != null,
    status = attributes.get(HttpAttributes.HTTP_RESPONSE_STATUS_CODE)?.toInt()
  )
}

private fun SpanData.toWorkerSpan(traceId: TraceId) = WorkerSpan(
  traceId = traceId,
  durationMs = durationMs,
  name = name,
  isError = status.statusCode == StatusCode.ERROR
)

private fun SpanData.includedAttributes(info: SpanInfo) =
  attributes.asMap().filterKeys { info.includeAttribute(it.key) }

private fun Map<AttributeKey<*>, Any>.toLogfmt() = map { (k, v) ->
  "${k.key}=${escapeLogfmt(v.toString())}"
}.sorted().joinToString(SEPARATOR)

private fun Map<AttributeKey<*>, Any>.toPrettyString() = map { (k, v) ->
  "${k.key}=${quoteValue(v.toString())}"
}.sorted().joinToString(SEPARATOR)

private fun escapeLogfmt(value: String): String {
  val needsQuoting = value.contains(' ') || value.contains('=') || value.contains('"') || value.contains('\n')
  if (!needsQuoting) return value
  return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
}

private fun quoteValue(value: String): String {
  val needsQuoting = value.contains(' ') || value.contains('=') || value.contains('"')
  if (!needsQuoting) return value
  return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

private fun SpanData.extractException(): ExceptionInfo? {
  val exceptionEvent = events.find { it.name == "exception" } ?: return null
  val attrs = exceptionEvent.attributes

  return ExceptionInfo(
    type = attrs.get(ExceptionAttributes.EXCEPTION_TYPE) ?: "Unknown",
    message = attrs.get(ExceptionAttributes.EXCEPTION_MESSAGE) ?: "",
    stacktrace = attrs.get(ExceptionAttributes.EXCEPTION_STACKTRACE)
  )
}

private data class ExceptionInfo(
  val type: String,
  val message: String,
  val stacktrace: String?
)

private fun ExceptionInfo.toLogfmt() = buildString {
  append("exception.type=$type")
  append(" exception.message=${escapeLogfmt(message)}")
  stacktrace?.let { append(" exception.stacktrace=${escapeLogfmt(it)}") }
}

private const val SEPARATOR = " "
