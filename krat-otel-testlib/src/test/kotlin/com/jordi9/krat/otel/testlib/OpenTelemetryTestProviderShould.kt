package com.jordi9.krat.otel.testlib

import com.jordi9.krat.otel.LogFormat
import com.jordi9.krat.otel.OpenTelemetryConfig
import com.jordi9.krat.otel.withSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class OpenTelemetryTestProviderShould : StringSpec({

  "captures finished spans" {
    val testProvider = OpenTelemetryTestProvider(
      OpenTelemetryConfig(serviceName = "test-service", logFormat = LogFormat.NONE)
    )

    val tracer = testProvider.provider.get().getTracer("test")
    tracer.withSpan("test-span") {
      setAttribute("key", "value")
    }

    testProvider.finishedSpans shouldHaveSize 1
    testProvider.finishedSpans.first().name shouldBe "test-span"
  }

  "reset clears captured spans" {
    val testProvider = OpenTelemetryTestProvider(
      OpenTelemetryConfig(serviceName = "test-service", logFormat = LogFormat.NONE)
    )

    val tracer = testProvider.provider.get().getTracer("test")

    tracer.withSpan("span-1") {}
    testProvider.finishedSpans shouldHaveSize 1

    testProvider.reset()
    testProvider.finishedSpans.shouldBeEmpty()

    tracer.withSpan("span-2") {}
    testProvider.finishedSpans shouldHaveSize 1
    testProvider.finishedSpans.first().name shouldBe "span-2"
  }
})
