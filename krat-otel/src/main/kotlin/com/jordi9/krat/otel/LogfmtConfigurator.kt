package com.jordi9.krat.otel

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.PatternLayout
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory

internal fun configureLogfmt() {
  val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

  rootLogger.removeConsoleAppenders()
  rootLogger.addAppender(buildLogfmtAppender())
}

internal fun buildLogfmtEncoder(): PatternLayoutEncoder {
  PatternLayout.DEFAULT_CONVERTER_MAP[MDC_CONVERTER_KEY] = LogfmtMdcConverter::class.java.name
  return PatternLayoutEncoder().apply {
    context = LoggerFactory.getILoggerFactory() as? LoggerContext
    pattern = LOGFMT_PATTERN
    start()
  }
}

private fun buildLogfmtAppender() = ConsoleAppender<ILoggingEvent>().apply {
  name = "CONSOLE"
  context = LoggerFactory.getILoggerFactory() as? LoggerContext
  encoder = buildLogfmtEncoder()
  start()
}

private fun Logger.removeConsoleAppenders() {
  iteratorForAppenders()
    .asSequence()
    .filterIsInstance<ConsoleAppender<ILoggingEvent>>()
    .toList()
    .forEach {
      it.stop()
      detachAppender(it)
    }
}

private const val MDC_CONVERTER_KEY = "logfmtMdc"

private const val LOGFMT_PATTERN =
  "ts=%date{yyyy-MM-dd'T'HH:mm:ss.SSS} level=%-5level thread=%thread logger=%logger%$MDC_CONVERTER_KEY msg=%m%n"

/**
 * Outputs all MDC entries as space-separated `key=value` pairs with a leading space when non-empty,
 * empty string otherwise. Logback's default `%mdc` uses `, ` between entries which is invalid logfmt.
 */
internal class LogfmtMdcConverter : ClassicConverter() {
  override fun convert(event: ILoggingEvent): String {
    val mdc = event.mdcPropertyMap
    if (mdc.isNullOrEmpty()) return ""
    return buildString {
      for ((k, v) in mdc) {
        append(' ')
        append(k)
        append('=')
        append(escape(v))
      }
    }
  }

  private fun escape(value: String): String {
    val needs = value.contains(' ') || value.contains('=') || value.contains('"') || value.contains('\n')
    if (!needs) return value
    return "\"${value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}\""
  }
}
