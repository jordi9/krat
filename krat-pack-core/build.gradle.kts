plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Ktor handler, route, service, and health check primitives"

dependencies {
  api(platform(libs.ktor.bom))
  api(libs.ktor.server.core)
  api(libs.ktor.server.sse)

  implementation(libs.kotlin.logging)
  implementation(libs.slf4j.api)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.ktor.server.test.host)
}
