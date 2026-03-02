package com.jordi9.krat.otel

import com.jordi9.krat.logging.LogEventsExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes

class OpenTelemetryProviderTest : StringSpec({

  val logs = extension(LogEventsExtension("TraceLog"))

  "creates valid OpenTelemetry instance" {
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(serviceName = "test-service"),
      spanProcessor = noopProcessor()
    )

    val otel = provider.get()

    otel shouldNotBe null
    provider.close()
  }

  "configures resource attributes correctly" {
    val exporter = InMemorySpanExporter.create()
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(
        serviceName = "my-service",
        serviceVersion = "2025.01.15",
        logFormat = LogFormat.NONE
      ),
      spanProcessor = SimpleSpanProcessor.create(exporter)
    )

    val tracer = provider.get().getTracer("test")
    tracer.spanBuilder("test-span").startSpan().end()

    val span = exporter.finishedSpanItems.single()
    span.resource.getAttribute(ServiceAttributes.SERVICE_NAME) shouldBe "my-service"
    span.resource.getAttribute(ServiceAttributes.SERVICE_VERSION) shouldBe "2025.01.15"

    provider.close()
  }

  "with LogFormat.NONE produces no logs" {
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(serviceName = "silent-service", logFormat = LogFormat.NONE)
    )

    val tracer = provider.get().getTracer("test")
    tracer.spanBuilder("worker.test")
      .setSpanKind(SpanKind.CONSUMER)
      .startSpan()
      .end()

    logs.events.size shouldBe 0
    provider.close()
  }

  "OpenTelemetryConfig has sensible defaults" {
    val config = OpenTelemetryConfig(serviceName = "my-app")

    config.otlpEndpoint shouldBe "http://localhost:4317"
    config.otlpEnabled shouldBe false
    config.logFormat shouldBe LogFormat.PRETTY
  }
})

private fun noopProcessor() = SimpleSpanProcessor.create(InMemorySpanExporter.create())
