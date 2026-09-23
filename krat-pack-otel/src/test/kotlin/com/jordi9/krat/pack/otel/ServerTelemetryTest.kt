package com.jordi9.krat.pack.otel

import com.jordi9.krat.otel.OpenTelemetryConfig
import com.jordi9.krat.otel.testlib.OpenTelemetryTestProvider
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class ServerTelemetryTest : StringSpec({
  "a child span after suspension stays in its HTTP request trace" {
    OpenTelemetryTestProvider(OpenTelemetryConfig(serviceName = "test-service")).use { telemetry ->
      testApplication {
        application {
          install(KtorServerTelemetry) { setOpenTelemetry(telemetry.provider.get()) }
          val tracer = telemetry.provider.get().getTracer("test")
          routing {
            get("/suspend") {
              delay(10)
              tracer.spanBuilder("db.query").startSpan().end()
              call.respond(HttpStatusCode.OK, "ok")
            }
          }
        }
        client.get("/suspend").status shouldBe HttpStatusCode.OK
      }

      val server = telemetry.finishedSpans.single { it.name == "GET /suspend" }
      val child = telemetry.finishedSpans.single { it.name == "db.query" }
      child.traceId shouldBe server.traceId
      child.parentSpanId shouldBe server.spanId
    }
  }

  "suspended requests retain separate trace contexts without tracing excluded health checks" {
    OpenTelemetryTestProvider(OpenTelemetryConfig(serviceName = "test-service")).use { telemetry ->
      testApplication {
        application {
          install(KtorServerTelemetry) { setOpenTelemetry(telemetry.provider.get()) }
          val tracer = telemetry.provider.get().getTracer("test")
          routing {
            get("/items/{id}") {
              delay(20)
              tracer.spanBuilder("item.${call.parameters["id"]}").startSpan().end()
              call.respond(HttpStatusCode.OK, "ok")
            }
            get("/health/readiness") {
              delay(20)
              tracer.spanBuilder("health.query").startSpan().end()
              call.respond(HttpStatusCode.OK, "ready")
            }
          }
        }
        coroutineScope {
          val first = async(Dispatchers.Default) {
            client.get("/items/1") { header("traceparent", "00-11111111111111111111111111111111-1111111111111111-01") }
          }
          val second = async(Dispatchers.Default) {
            client.get("/items/2") { header("traceparent", "00-22222222222222222222222222222222-2222222222222222-01") }
          }
          first.await().status shouldBe HttpStatusCode.OK
          second.await().status shouldBe HttpStatusCode.OK
        }
        client.get("/health/readiness").status shouldBe HttpStatusCode.OK
      }

      for (id in 1..2) {
        val traceId = "$id".repeat(32)
        val server = telemetry.finishedSpans.single { it.name == "GET /items/{id}" && it.traceId == traceId }
        val child = telemetry.finishedSpans.single { it.name == "item.$id" }
        child.traceId shouldBe server.traceId
        child.parentSpanId shouldBe server.spanId
      }
      telemetry.finishedSpans.size shouldBe 4
    }
  }
})
