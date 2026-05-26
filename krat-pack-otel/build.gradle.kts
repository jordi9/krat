plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "OpenTelemetry startup tracing for Ktor applications"

dependencies {
  api(project(":krat-otel"))
  api(platform(libs.ktor.bom))
  api(libs.ktor.server.core)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.ktor.server.test.host)
  testImplementation(project(":krat-otel-testlib"))
}
