package com.jordi9.krat.pack.cors

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.cors.routing.CORS

fun Application.installCors(config: CorsConfig) {
  install(CORS) {
    config.allowedHosts.forEach { allowHost(it) }
    if (config.allowAnyLocalhost) {
      allowOrigins(::isLocalhost)
    }
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Patch)
    allowMethod(HttpMethod.Delete)
    allowHeader(HttpHeaders.ContentType)
  }
}

private fun isLocalhost(origin: String): Boolean = isLocalhost(origin, "localhost") ||
  isLocalhost(origin, "127.0.0.1")

private fun isLocalhost(origin: String, host: String): Boolean = origin.startsWith("http://$host:") ||
  origin.startsWith("https://$host:")
