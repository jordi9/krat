package com.jordi9.krat.otel

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import org.slf4j.LoggerFactory

fun maybeConfigureLogfmt(format: LogFormat) {
  if (format != LogFormat.LOGFMT) return

  val context = LoggerFactory.getILoggerFactory() as? LoggerContext ?: return
  val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)

  val logfmtAppender = ConsoleAppender<ILoggingEvent>().apply {
    name = "CONSOLE"
    this.context = context
    encoder = PatternLayoutEncoder().apply {
      this.context = context
      pattern = LOGFMT_PATTERN
      start()
    }
    start()
  }

  rootLogger.removeConsoleAppenders()
  rootLogger.addAppender(logfmtAppender)
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

private const val LOGFMT_PATTERN =
  "ts=%date{yyyy-MM-dd'T'HH:mm:ss.SSS} level=%-5level thread=%thread logger=%logger msg=%m%mdc%n"
