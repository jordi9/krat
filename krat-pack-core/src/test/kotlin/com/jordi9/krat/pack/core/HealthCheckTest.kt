package com.jordi9.krat.pack.core

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class HealthCheckTest : StringSpec({

  "GET /health returns 200 OK when all checks are healthy" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("check1") { HealthCheckResult.healthy("Check 1 OK") },
              HealthCheck("check2") { HealthCheckResult.healthy("Check 2 OK") }
            )
          )
        }
      }

      val response = client.get("/health")
      response.status shouldBe HttpStatusCode.OK
      response.contentType()?.withoutParameters() shouldBe ContentType.Text.Plain
      val body = response.bodyAsText()
      body shouldContain "check1 : HEALTHY [Check 1 OK]"
      body shouldContain "check2 : HEALTHY [Check 2 OK]"
      body shouldContain "Available checks: /health, /health/liveness, /health/readiness, /health/{name}"
    }
  }

  "GET /health returns 503 Service Unavailable when any check is unhealthy" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("healthy-check") { HealthCheckResult.healthy("OK") },
              HealthCheck("unhealthy-check") { HealthCheckResult.unhealthy("Service down") }
            )
          )
        }
      }

      val response = client.get("/health")
      response.status shouldBe HttpStatusCode.ServiceUnavailable
      response.contentType()?.withoutParameters() shouldBe ContentType.Text.Plain
      val body = response.bodyAsText()
      body shouldContain "healthy-check : HEALTHY [OK]"
      body shouldContain "unhealthy-check : UNHEALTHY [Service down]"
    }
  }

  "GET /health returns 404 when no checks are provided" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = emptyList()
          )
        }
      }

      val response = client.get("/health")
      response.status shouldBe HttpStatusCode.NotFound
      response.contentType()?.withoutParameters() shouldBe ContentType.Text.Plain
      response.bodyAsText() shouldBe "No health checks found"
    }
  }

  "GET /health/liveness returns 200 OK with healthy status" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(HealthCheck("check1") { HealthCheckResult.healthy("OK") })
          )
        }
      }

      val response = client.get("/health/liveness")
      response.status shouldBe HttpStatusCode.OK
      response.contentType()?.withoutParameters() shouldBe ContentType.Text.Plain
      response.bodyAsText() shouldBe "liveness: HEALTHY"
    }
  }

  "GET /health/readiness returns only readiness checks" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("database") { HealthCheckResult.healthy("Database OK") },
              HealthCheck("cache") { HealthCheckResult.healthy("Cache OK") }
            ),
            readiness = listOf(
              HealthCheck("database") { HealthCheckResult.healthy("Database OK") }
            )
          )
        }
      }

      val response = client.get("/health/readiness")
      response.status shouldBe HttpStatusCode.OK
      val body = response.bodyAsText()
      body shouldContain "database : HEALTHY [Database OK]"
      body shouldNotContain "cache"
    }
  }

  "GET /health/{name} returns individual check by name" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("database") { HealthCheckResult.healthy("Database OK") },
              HealthCheck("queue") { HealthCheckResult.healthy("Queue OK") }
            )
          )
        }
      }

      val response = client.get("/health/database")
      response.status shouldBe HttpStatusCode.OK
      val body = response.bodyAsText()
      body shouldContain "database : HEALTHY [Database OK]"
      body shouldNotContain "queue"
    }
  }

  "GET /health/{name} returns 404 when check name doesn't exist" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("database") { HealthCheckResult.healthy("Database OK") }
            )
          )
        }
      }

      val response = client.get("/health/nonexistent")
      response.status shouldBe HttpStatusCode.NotFound
      response.bodyAsText() shouldBe "No health checks found"
    }
  }

  "handler catches exceptions thrown by health checks" {
    testApplication {
      application {
        routing {
          healthChecks(
            basePath = "/health",
            checks = listOf(
              HealthCheck("failing-check") {
                throw RuntimeException("Something went wrong")
              }
            )
          )
        }
      }

      val response = client.get("/health")
      response.status shouldBe HttpStatusCode.ServiceUnavailable
      val body = response.bodyAsText()
      body shouldContain "failing-check : UNHEALTHY"
      body shouldContain "Something went wrong"
    }
  }

  // Note: CancellationException propagation is verified manually during hot reload testing
  // It's difficult to test via HTTP layer because Ktor's test framework handles cancellation
  // differently than production. The fix in HealthCheckHandler ensures that:
  //   1. CancellationException is caught BEFORE the generic Exception handler
  //   2. It's immediately re-thrown to allow proper structured concurrency
  // This prevents "Job was cancelled!" from appearing as UNHEALTHY during server shutdown

  "health check with custom name uses provided name" {
    val check = HealthCheck("custom-name") {
      HealthCheckResult.healthy("Custom check OK")
    }

    check.name shouldBe "custom-name"
  }

  "HealthCheckResult.healthy creates healthy result" {
    val result = HealthCheckResult.healthy("All good")
    result.healthy shouldBe true
    result.message shouldBe "All good"
    result.cause shouldBe null
  }

  "HealthCheckResult.unhealthy with message creates unhealthy result" {
    val result = HealthCheckResult.unhealthy("Something failed")
    result.healthy shouldBe false
    result.message shouldBe "Something failed"
    result.cause shouldBe null
  }

  "HealthCheckResult.unhealthy with exception creates unhealthy result with cause" {
    val exception = RuntimeException("Error occurred")
    val result = HealthCheckResult.unhealthy(exception)
    result.healthy shouldBe false
    result.message shouldBe "Error occurred"
    result.cause shouldBe exception
  }
})
