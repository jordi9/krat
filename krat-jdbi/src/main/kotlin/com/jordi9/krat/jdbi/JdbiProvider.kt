package com.jordi9.krat.jdbi

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.composite.CompositeMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.opentelemetry.JdbiOpenTelemetryPlugin

/**
 * Creates a configured JDBI instance with HikariCP connection pooling.
 *
 * @param config Database configuration
 * @param openTelemetry OpenTelemetry instance for query tracing. Defaults to noop (no tracing).
 * @param meterRegistry Micrometer registry for HikariCP pool metrics. Defaults to empty composite (no metrics).
 */
class JdbiProvider(
  private val config: DatabaseConfig,
  private val openTelemetry: OpenTelemetry = OpenTelemetry.noop(),
  private val meterRegistry: MeterRegistry = CompositeMeterRegistry()
) : AutoCloseable {

  private val dataSource: HikariDataSource by lazy {
    HikariDataSource(
      HikariConfig().apply {
        jdbcUrl = config.url
        username = config.user
        password = config.password
        maximumPoolSize = config.maximumPoolSize
        metricRegistry = meterRegistry
      }
    )
  }

  private val jdbi: Jdbi by lazy {
    Jdbi.create(dataSource).apply {
      installPlugin(KotlinPlugin())
      installPlugin(JdbiOpenTelemetryPlugin(openTelemetry))
    }
  }

  fun get(): Jdbi = jdbi

  override fun close() {
    dataSource.close()
  }
}
