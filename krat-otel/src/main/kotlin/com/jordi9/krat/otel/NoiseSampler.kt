package com.jordi9.krat.otel

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.data.LinkData
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingResult
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes

/**
 * A [Sampler] that excludes health check routes from tracing.
 *
 * Health checks generate high-volume, low-signal telemetry that adds noise and increases
 * observability costs. This sampler drops spans for configured routes **before** they are
 * created, affecting all downstream processors and exporters.
 *
 * ```
 * Request → Sampler (DROP?) → SpanProcessor(s) → Export
 *               ↓
 *          LoggingSpanProcessor
 *          BatchSpanProcessor (OTLP)
 * ```
 *
 * @param delegate The sampler to delegate to for non-excluded spans.
 *   Defaults to [Sampler.parentBased] with [Sampler.alwaysOn].
 * @param excludeRoutes Routes to exclude from tracing. Matched against the `http.route` attribute.
 *
 * @see <a href="https://opentelemetry.io/blog/2025/declarative-config/">OpenTelemetry: The Declarative Configuration Journey</a>
 */
class NoiseSampler(
  private val delegate: Sampler = Sampler.parentBased(Sampler.alwaysOn()),
  private val excludeRoutes: Set<String> = DEFAULT_EXCLUDED_ROUTES
) : Sampler {

  override fun shouldSample(
    parentContext: Context,
    traceId: String,
    name: String,
    spanKind: SpanKind,
    attributes: Attributes,
    parentLinks: List<LinkData>
  ): SamplingResult {
    if (spanKind == SpanKind.SERVER) {
      // Check url.path first (available at sampling time), fall back to http.route
      val path = attributes.get(UrlAttributes.URL_PATH) ?: attributes.get(HttpAttributes.HTTP_ROUTE)
      if (path in excludeRoutes) {
        return SamplingResult.drop()
      }
    }
    return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks)
  }

  override fun getDescription() = "NoiseSampler{excludeRoutes=$excludeRoutes}"

  companion object {
    val DEFAULT_EXCLUDED_ROUTES = setOf(
      "/health",
      "/health/liveness",
      "/health/readiness",
      "/healthz",
      "/livez",
      "/readyz",
      "/ready",
      "/metrics"
    )
  }
}
