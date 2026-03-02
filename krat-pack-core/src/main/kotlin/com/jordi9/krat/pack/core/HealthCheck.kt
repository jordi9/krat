package com.jordi9.krat.pack.core

/**
 * Result of a health check.
 */
data class HealthCheckResult(
  val healthy: Boolean,
  val message: String,
  val cause: Throwable? = null
) {
  companion object {
    fun healthy(message: String = "OK") = HealthCheckResult(true, message, null)

    fun unhealthy(message: String, cause: Throwable? = null) = HealthCheckResult(false, message, cause)

    fun unhealthy(cause: Throwable) = HealthCheckResult(false, cause.message ?: "Unknown error", cause)
  }
}

/**
 * Port interface for health checks.
 *
 * Health checks verify that system dependencies are operational.
 * Each check should be fast (<5 seconds) and idempotent.
 */
fun interface HealthCheck {
  suspend fun check(): HealthCheckResult

  val name: String
    get() = this::class.simpleName ?: this::class.java.simpleName

  companion object {
    operator fun invoke(name: String, check: suspend () -> HealthCheckResult): HealthCheck = object : HealthCheck {
      override val name: String = name

      override suspend fun check(): HealthCheckResult = check()
    }
  }
}
