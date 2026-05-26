plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
  alias(libs.plugins.kotlin.serialization)
}

group = "com.jordi9"
description = "OpenTelemetry provider, tracer extensions, span utilities, and canonical trace logging for Ktor"

dependencies {
  api(platform(libs.opentelemetry.bom))
  api(libs.opentelemetry.api)
  api(libs.opentelemetry.extension.kotlin)
  api(libs.kotlinx.serialization.json)

  implementation(libs.opentelemetry.sdk)
  implementation(libs.opentelemetry.exporter.otlp)
  implementation(libs.opentelemetry.semconv)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.caffeine)
  implementation(libs.slf4j.api)
  implementation(libs.slf4j.jul)
  implementation(libs.logback)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(platform(libs.ktor.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.opentelemetry.sdk.testing)
  testImplementation(libs.ktor.server.test.host)
  testImplementation(libs.opentelemetry.ktor)
  testImplementation(libs.opentelemetry.proto)
  testImplementation(libs.grpc.netty.shaded)
  testImplementation(libs.grpc.protobuf)
  testImplementation(libs.grpc.stub)
  testImplementation(project(":krat-logging-testlib"))
}
