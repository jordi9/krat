package com.jordi9.krat.logging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(LogEventsExtensionCustomLoggerTest::class.java)

class LogEventsExtensionCustomLoggerTest : StringSpec({

  val logs = extension(LogEventsExtension(LoggerFactory.getLogger(LogEventsExtensionCustomLoggerTest::class.java)))

  "only captures events from the specified logger" {
    log.info("Howdy")

    logs.events shouldHaveSize 1
    logs.events.shouldExist { it.message == "Howdy" }
  }

  "ignores other loggers" {
    LoggerFactory.getLogger("SomeOtherLogger").info("Should be ignored")

    logs.events.shouldBeEmpty()
  }
})
