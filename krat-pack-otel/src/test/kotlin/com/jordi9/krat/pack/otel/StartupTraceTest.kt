package com.jordi9.krat.pack.otel

import com.jordi9.krat.otel.OpenTelemetryConfig
import com.jordi9.krat.otel.testlib.OpenTelemetryTestProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication
import io.opentelemetry.api.trace.SpanKind

class StartupTraceTest : StringSpec({

  "startupTracer creates a span that ends when the application starts" {
    val otel = OpenTelemetryTestProvider(OpenTelemetryConfig(serviceName = "test-service"))

    testApplication {
      application {
        startupTracer(otel.provider.get()) { tracer ->
          tracer.spanBuilder("child-span").startSpan().end()
        }
      }
    }

    otel.finishedSpans shouldHaveSize 2

    val startupSpan = otel.finishedSpans
      .first { it.name == "app-start" }
      .apply {
        kind shouldBe SpanKind.INTERNAL
        instrumentationScopeInfo.name shouldBe "krat-otel"
      }

    otel.finishedSpans.first { it.name == "child-span" }.parentSpanId shouldBe startupSpan.spanContext.spanId

    otel.close()
  }
})
