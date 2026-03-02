package com.jordi9.krat.pack.core

/**
 * A service participates in the application lifecycle.
 *
 * Services are started after the application is configured but before it begins accepting requests.
 * Services are stopped during application shutdown, after the server stops accepting new requests.
 *
 * Inspired by Ratpack's Service interface.
 */
interface Service {
  suspend fun start()

  suspend fun stop()

  fun getName(): String = this::class.simpleName ?: "Unknown"
}
