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
      allowOrigins { it.startsWith("http://localhost:") }
    }
    allowMethod(HttpMethod.Options)
    allowMethod(HttpMethod.Patch)
    allowMethod(HttpMethod.Delete)
    allowHeader(HttpHeaders.ContentType)
  }
}
