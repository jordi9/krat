package com.jordi9.krat.otel.testlib

import com.jordi9.krat.otel.OpenTelemetryConfig
import com.jordi9.krat.otel.OpenTelemetryProvider
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor

class OpenTelemetryTestProvider(
  val config: OpenTelemetryConfig
) : AutoCloseable {

  val spanExporter: InMemorySpanExporter = InMemorySpanExporter.create()

  val provider: OpenTelemetryProvider = OpenTelemetryProvider(
    config = config,
    spanProcessor = SimpleSpanProcessor.create(spanExporter)
  )

  val finishedSpans: List<SpanData>
    get() = spanExporter.finishedSpanItems

  fun reset() {
    spanExporter.reset()
  }

  override fun close() {
    provider.close()
  }
}
