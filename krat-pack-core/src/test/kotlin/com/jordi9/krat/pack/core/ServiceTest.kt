package com.jordi9.krat.pack.core

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import io.ktor.server.testing.testApplication

class ServiceTest : StringSpec() {
  override fun isolationMode() = IsolationMode.InstancePerRoot

  val lifecycle = mutableListOf<String>()

  init {

    "installService starts a single service" {
      testApplication {
        application {
          installService(TestService("service1", lifecycle))
        }
      }

      lifecycle shouldContainInOrder listOf("service1 started", "service1 stopped")
    }

    "installService starts multiple services in order" {
      testApplication {
        application {
          installService(TestService("service1", lifecycle))
          installService(TestService("service2", lifecycle))
          installService(TestService("service3", lifecycle))
        }
      }

      lifecycle shouldContainInOrder listOf(
        "service1 started",
        "service2 started",
        "service3 started",
        "service1 stopped",
        "service2 stopped",
        "service3 stopped"
      )
    }

    "installServices starts all services in order" {
      testApplication {
        application {
          val services = Services(
            listOf(
              TestService("service1", lifecycle),
              TestService("service2", lifecycle),
              TestService("service3", lifecycle)
            )
          )
          installServices(services)
        }
      }

      lifecycle shouldContainInOrder listOf(
        "service1 started",
        "service2 started",
        "service3 started",
        "service1 stopped",
        "service2 stopped",
        "service3 stopped"
      )
    }

    "service stop errors are isolated and don't prevent other services from stopping" {
      testApplication {
        application {
          installService(TestService("service1", lifecycle))
          installService(TestService("service2", lifecycle, shouldFailOnStop = true))
          installService(TestService("service3", lifecycle))
        }
      }

      // All services should start
      lifecycle.filter { it.contains("started") } shouldContainInOrder listOf(
        "service1 started",
        "service2 started",
        "service3 started"
      )

      // service2 fails to stop, but service1 and service3 still stop
      lifecycle.filter { it.contains("stopped") } shouldContainInOrder listOf(
        "service1 stopped",
        "service3 stopped"
      )
    }

    "service getName returns custom name" {
      val service = TestService("my-custom-service", mutableListOf())
      service.getName() shouldBe "my-custom-service"
    }

    "Services can be mixed - installService and installServices work independently" {
      testApplication {
        application {
          // Install some services individually
          installService(TestService("individual1", lifecycle))
          installService(TestService("individual2", lifecycle))

          // Install some services as a batch
          val services = Services(
            listOf(
              TestService("batch1", lifecycle),
              TestService("batch2", lifecycle)
            )
          )
          installServices(services)
        }
      }

      lifecycle shouldContainInOrder listOf(
        "individual1 started",
        "individual2 started",
        "batch1 started",
        "batch2 started",
        "individual1 stopped",
        "individual2 stopped",
        "batch1 stopped",
        "batch2 stopped"
      )
    }
  }
}

/**
 * Test service that tracks its lifecycle state.
 * Used to verify that services are started and stopped correctly.
 */
class TestService(
  private val name: String,
  private val lifecycle: MutableList<String>,
  private val shouldFailOnStart: Boolean = false,
  private val shouldFailOnStop: Boolean = false
) : Service {

  override suspend fun start() {
    if (shouldFailOnStart) {
      throw RuntimeException("Start failed for $name")
    }
    lifecycle.add("$name started")
  }

  override suspend fun stop() {
    if (shouldFailOnStop) {
      throw RuntimeException("Stop failed for $name")
    }
    lifecycle.add("$name stopped")
  }
  override fun getName(): String = name
}
