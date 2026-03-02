package com.jordi9.krat.pack.core

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.util.AttributeKey
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

internal suspend fun startService(service: Service) {
  logger.info { "Starting service: ${service.getName()}" }
  try {
    service.start()
    logger.info { "Service started: ${service.getName()}" }
  } catch (e: Exception) {
    logger.error(e) { "Failed to start service: ${service.getName()}" }
    throw e
  }
}

internal suspend fun stopService(service: Service) {
  try {
    logger.info { "Stopping service: ${service.getName()}" }
    service.stop()
    logger.info { "Service stopped: ${service.getName()}" }
  } catch (e: Exception) {
    logger.error(e) { "Error stopping service: ${service.getName()}" }
  }
}

/**
 * Manages a collection of services and their lifecycle.
 */
class Services(
  private val services: List<Service>
) {
  suspend fun startAll() {
    services.forEach { startService(it) }
  }

  suspend fun stopAll() {
    services.forEach { stopService(it) }
  }
}

/**
 * Install services into the Ktor application lifecycle.
 */
fun Application.installServices(services: Services) {
  runBlocking { services.startAll() }

  monitor.subscribe(ApplicationStopping) {
    runBlocking { services.stopAll() }
  }
}

private val RegisteredServicesKey = AttributeKey<MutableList<Service>>("RegisteredServices")

/**
 * Install a single service into the Ktor application lifecycle.
 */
fun Application.installService(service: Service) {
  val servicesList =
    attributes.computeIfAbsent(RegisteredServicesKey) {
      mutableListOf<Service>().also {
        monitor.subscribe(ApplicationStopping) {
          runBlocking {
            attributes[RegisteredServicesKey].forEach { svc -> stopService(svc) }
          }
        }
      }
    }

  runBlocking { startService(service) }
  servicesList.add(service)
}
