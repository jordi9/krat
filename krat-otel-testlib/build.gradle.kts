plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "In-memory OpenTelemetry span capture for testing"

dependencies {
  api(project(":krat-otel"))
  api(platform(libs.opentelemetry.bom))
  api(libs.opentelemetry.sdk.testing)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
}
