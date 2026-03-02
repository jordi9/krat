package com.jordi9.krat.otel

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.sdk.trace.samplers.Sampler
import io.opentelemetry.sdk.trace.samplers.SamplingDecision
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes

class NoiseSamplerTest : StringSpec({

  "drops SERVER spans for default excluded routes" {
    val sampler = NoiseSampler()

    sample(sampler, "/health") shouldBe SamplingDecision.DROP
    sample(sampler, "/health/liveness") shouldBe SamplingDecision.DROP
    sample(sampler, "/health/readiness") shouldBe SamplingDecision.DROP
    sample(sampler, "/healthz") shouldBe SamplingDecision.DROP
    sample(sampler, "/livez") shouldBe SamplingDecision.DROP
    sample(sampler, "/readyz") shouldBe SamplingDecision.DROP
    sample(sampler, "/ready") shouldBe SamplingDecision.DROP
    sample(sampler, "/metrics") shouldBe SamplingDecision.DROP
  }

  "allows SERVER spans for non-excluded routes" {
    val sampler = NoiseSampler()

    sample(sampler, "/api/users") shouldBe SamplingDecision.RECORD_AND_SAMPLE
    sample(sampler, "/orders") shouldBe SamplingDecision.RECORD_AND_SAMPLE
    sample(sampler, "/healthy") shouldBe SamplingDecision.RECORD_AND_SAMPLE
  }

  "allows non-SERVER spans regardless of route" {
    val sampler = NoiseSampler()

    sample(sampler, "/health", SpanKind.CLIENT) shouldBe SamplingDecision.RECORD_AND_SAMPLE
    sample(sampler, "/health", SpanKind.INTERNAL) shouldBe SamplingDecision.RECORD_AND_SAMPLE
    sample(sampler, "/health", SpanKind.PRODUCER) shouldBe SamplingDecision.RECORD_AND_SAMPLE
    sample(sampler, "/health", SpanKind.CONSUMER) shouldBe SamplingDecision.RECORD_AND_SAMPLE
  }

  "allows SERVER spans without path attribute" {
    val sampler = NoiseSampler()

    sample(sampler, null) shouldBe SamplingDecision.RECORD_AND_SAMPLE
  }

  "checks http.route when url.path is not available" {
    val sampler = NoiseSampler()

    sample(sampler, "/health", useHttpRoute = true) shouldBe SamplingDecision.DROP
  }

  "uses custom excluded routes when provided" {
    val sampler = NoiseSampler(
      excludeRoutes = setOf("/ping", "/status")
    )

    sample(sampler, "/ping") shouldBe SamplingDecision.DROP
    sample(sampler, "/status") shouldBe SamplingDecision.DROP
    sample(sampler, "/health") shouldBe SamplingDecision.RECORD_AND_SAMPLE
  }

  "delegates to parent sampler for allowed spans" {
    val alwaysOffDelegate = Sampler.alwaysOff()
    val sampler = NoiseSampler(delegate = alwaysOffDelegate)

    sample(sampler, "/api/users") shouldBe SamplingDecision.DROP
  }

  "provides descriptive description" {
    val sampler = NoiseSampler(excludeRoutes = setOf("/health"))

    sampler.description shouldContain "NoiseSampler"
    sampler.description shouldContain "/health"
  }
})

private fun sample(
  sampler: Sampler,
  path: String?,
  spanKind: SpanKind = SpanKind.SERVER,
  useHttpRoute: Boolean = false
): SamplingDecision {
  val attributes = if (path != null) {
    Attributes.of(if (useHttpRoute) HttpAttributes.HTTP_ROUTE else UrlAttributes.URL_PATH, path)
  } else {
    Attributes.empty()
  }
  return sampler.shouldSample(
    Context.root(),
    "00000000000000000000000000000001",
    "test-span",
    spanKind,
    attributes,
    emptyList()
  ).decision
}
