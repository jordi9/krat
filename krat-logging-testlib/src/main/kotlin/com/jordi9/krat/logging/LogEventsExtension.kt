package com.jordi9.krat.logging

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import org.slf4j.Logger
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger as LogbackLogger

/**
 * Kotest extension that captures log events during test execution.
 *
 * Usage:
 * ```kotlin
 * class MyTest : StringSpec({
 *   val logs = extension(LogEventsExtension())
 *
 *   "should log something" {
 *     myService.doSomething()
 *     logs.events.shouldHaveSize(1)
 *     logs.events.first().message shouldBe "Expected message"
 *   }
 * })
 * ```
 *
 * @param logger The logger to capture events from (defaults to root logger)
 */
class LogEventsExtension(
  logger: Logger = LoggerFactory.getLogger(ROOT_LOGGER_NAME)
) : TestListener {

  constructor(name: String) : this(LoggerFactory.getLogger(name))

  private val logger: LogbackLogger = logger as LogbackLogger
  private var appender = ListAppender<ILoggingEvent>()

  val events: List<ILoggingEvent>
    get() = appender.list

  override suspend fun beforeTest(testCase: TestCase) {
    appender = ListAppender<ILoggingEvent>()
    appender.start()
    logger.addAppender(appender)
  }

  override suspend fun afterTest(testCase: TestCase, result: TestResult) {
    logger.detachAppender(appender)
  }
}
