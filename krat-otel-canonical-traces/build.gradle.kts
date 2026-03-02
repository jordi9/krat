plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Canonical trace log line processor for OpenTelemetry spans"

dependencies {
  api(project(":krat-otel"))
  api(platform(libs.opentelemetry.bom))

  implementation(libs.opentelemetry.sdk)
  implementation(libs.opentelemetry.semconv)
  implementation(libs.caffeine)
  implementation(libs.slf4j.api)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(platform(libs.ktor.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.opentelemetry.sdk.testing)
  testImplementation(libs.ktor.server.test.host)
  testImplementation(libs.logback)
  testImplementation(libs.opentelemetry.ktor)
  testImplementation(project(":krat-logging-testlib"))
}
