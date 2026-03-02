package com.jordi9.krat.logging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldExist
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(LogEventsExtensionTest::class.java)

class LogEventsExtensionTest : StringSpec({

  val logs = extension(LogEventsExtension())

  "captures log events during test" {
    log.info("Hello")
    log.debug("World")

    logs.events shouldHaveSize 2
    logs.events.shouldExist { it.message == "Hello" }
    logs.events.shouldExist { it.message == "World" }
  }

  "clears events between tests" {
    // This test runs after the previous one - events should be cleared
    log.info("Fresh start")

    logs.events shouldHaveSize 1
    logs.events.first().message shouldBe "Fresh start"
  }

  "captures log level" {
    log.info("info message")
    log.warn("warn message")
    log.error("error message")

    logs.events shouldHaveSize 3
    logs.events[0].level.toString() shouldBe "INFO"
    logs.events[1].level.toString() shouldBe "WARN"
    logs.events[2].level.toString() shouldBe "ERROR"
  }

  "events list is empty when nothing is logged" {
    // Don't log anything
    logs.events.shouldBeEmpty()
  }
})
