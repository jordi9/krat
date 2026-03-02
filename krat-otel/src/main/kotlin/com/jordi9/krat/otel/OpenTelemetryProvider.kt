package com.jordi9.krat.otel

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import kotlinx.serialization.Serializable
import org.slf4j.bridge.SLF4JBridgeHandler

class OpenTelemetryProvider(
  private val config: OpenTelemetryConfig,
  spanProcessor: SpanProcessor = defaultSpanProcessor(config)
) : AutoCloseable {

  init {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()

    maybeConfigureLogfmt(config.logFormat)
  }

  private val openTelemetrySdk: OpenTelemetrySdk by lazy {
    val resource = Resource.create(
      Attributes.builder()
        .put(ServiceAttributes.SERVICE_NAME, config.serviceName)
        .apply { config.serviceVersion?.let { put(ServiceAttributes.SERVICE_VERSION, it) } }
        .build()
    )

    val sampler = NoiseSampler(excludeRoutes = config.excludeRoutes)

    val tracerProvider = SdkTracerProvider.builder()
      .setSampler(sampler)
      .addSpanProcessor(spanProcessor)
      .setResource(resource)
      .build()

    OpenTelemetrySdk.builder()
      .setTracerProvider(tracerProvider)
      .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
      .build()
  }

  fun get(): OpenTelemetry = openTelemetrySdk

  override fun close() {
    openTelemetrySdk.close()
  }
}

private fun defaultSpanProcessor(config: OpenTelemetryConfig): SpanProcessor = if (config.otlpEnabled) {
  BatchSpanProcessor.builder(
    OtlpGrpcSpanExporter.builder().setEndpoint(config.otlpEndpoint).build()
  ).build()
} else {
  SpanProcessor.composite()
}

@Serializable
data class OpenTelemetryConfig(
  val serviceName: String,
  val serviceVersion: String? = null,
  val otlpEndpoint: String = "http://localhost:4317",
  val otlpEnabled: Boolean = false,
  val logFormat: LogFormat = LogFormat.PRETTY,
  val excludeRoutes: Set<String> = NoiseSampler.DEFAULT_EXCLUDED_ROUTES
)

@Serializable
enum class LogFormat {
  /** Disable trace logging */
  NONE,

  /** Multi-line pretty format with child spans - ideal for development */
  PRETTY,

  /** Single-line logfmt format - ideal for production log aggregation */
  LOGFMT
}
