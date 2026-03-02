package com.jordi9.krat.pack.core

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.util.getValue
import kotlinx.coroutines.CancellationException

class HealthCheckHandler(
  private val checks: List<HealthCheck>,
  private val basePath: String
) : Handler {

  override suspend fun handle(call: ApplicationCall) {
    if (checks.isEmpty()) {
      return call.respondText(
        text = "No health checks found",
        status = HttpStatusCode.NotFound
      )
    }

    val results = checks.map { check ->
      val result = try {
        check.check()
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        HealthCheckResult.unhealthy(e)
      }
      CheckResult(
        name = check.name,
        healthy = result.healthy,
        message = result.message
      )
    }

    call.respondText(
      text = buildResponseText(results),
      status = if (results.allHealthy()) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
    )
  }

  private fun buildResponseText(results: List<CheckResult>): String {
    val checks = results.joinToString(separator = "\n") { result ->
      val status = if (result.healthy) "HEALTHY" else "UNHEALTHY"
      "${result.name} : $status [${result.message}]"
    }

    val footer = """
      |
      |
      |Available checks: $basePath, $basePath/liveness, $basePath/readiness, $basePath/{name}
    """.trimMargin()

    return checks + footer
  }

  private fun List<CheckResult>.allHealthy(): Boolean = all { it.healthy }

  private data class CheckResult(
    val name: String,
    val healthy: Boolean,
    val message: String
  )
}

fun Route.healthChecks(basePath: String, checks: List<HealthCheck>, readiness: List<HealthCheck> = emptyList()) {
  get(basePath, HealthCheckHandler(checks, basePath))
  get("$basePath/{name}") { handleIndividualCheck(checks, basePath) }
  get("$basePath/readiness", HealthCheckHandler(readiness, basePath))
  get("$basePath/liveness") { handleLiveness() }
}

private suspend fun RoutingContext.handleLiveness() {
  call.respondText(
    text = "liveness: HEALTHY",
    status = HttpStatusCode.OK
  )
}

private suspend fun RoutingContext.handleIndividualCheck(allChecks: List<HealthCheck>, basePath: String) {
  val name: String by call.parameters

  HealthCheckHandler(
    checks = allChecks.filter { it.name == name },
    basePath = basePath
  ).handle(call)
}
