package com.jordi9.krat.jdbi

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import org.jdbi.v3.core.kotlin.mapTo
import java.io.File

class JdbiProviderTest : StringSpec() {

  override fun isolationMode(): IsolationMode = IsolationMode.InstancePerRoot

  val dbFile: File = tempfile(prefix = "test-", suffix = ".db")
  val jdbcUrl = "jdbc:sqlite:${dbFile.absolutePath}"
  val config = DatabaseConfig(jdbcUrl)

  init {
    "creates JDBI instance with HikariCP" {
      val provider = JdbiProvider(config)

      val jdbi = provider.get()

      val result = jdbi.handleSync {
        createQuery("SELECT 1 as value").mapTo<Int>().one()
      }
      result shouldBe 1
    }

    "installs KotlinPlugin for data class mapping" {
      val provider = JdbiProvider(config)
      val jdbi = provider.get()

      jdbi.useSync {
        execute("CREATE TABLE test (id INTEGER PRIMARY KEY, name TEXT)")
        execute("INSERT INTO test (id, name) VALUES (1, 'hello')")
      }

      data class TestRow(val id: Int, val name: String)

      val result = jdbi.handleSync {
        createQuery("SELECT id, name FROM test").mapTo<TestRow>().one()
      }
      result shouldBe TestRow(1, "hello")
    }

    "works without OpenTelemetry (uses noop default)" {
      val provider = JdbiProvider(config)

      val result = provider.get().handleSync {
        createQuery("SELECT 42 as value").mapTo<Int>().one()
      }
      result shouldBe 42
    }

    "works with OpenTelemetry" {
      val provider = JdbiProvider(
        config = config,
        openTelemetry = OpenTelemetrySdk.builder().build()
      )

      val result = provider.get().handleSync {
        createQuery("SELECT 42 as value").mapTo<Int>().one()
      }
      result shouldBe 42
    }

    "respects maximumPoolSize configuration" {
      val registry = SimpleMeterRegistry()
      val provider = JdbiProvider(
        config = DatabaseConfig(url = jdbcUrl, maximumPoolSize = 5),
        meterRegistry = registry
      )

      // Trigger pool initialization
      provider.get().handleSync { createQuery("SELECT 1").mapTo<Int>().one() }

      // Verify max pool size via metrics
      val maxGauge = registry.find("hikaricp.connections.max").gauge()
      maxGauge?.value() shouldBe 5.0
    }

    "registers HikariCP metrics with MeterRegistry" {
      val registry = SimpleMeterRegistry()
      val provider = JdbiProvider(
        config = config,
        openTelemetry = OpenTelemetry.noop(),
        meterRegistry = registry
      )

      provider.get().handleSync { createQuery("SELECT 1").mapTo<Int>().one() }

      val hikariMeters = registry.meters.filter { it.id.name.startsWith("hikaricp") }
      hikariMeters.size shouldNotBe 0
    }
  }
}
