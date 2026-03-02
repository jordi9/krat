plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
  alias(libs.plugins.kotlin.serialization)
}

group = "com.jordi9"
description = "JDBI provider with HikariCP, Micrometer, and Loom support"

dependencies {
  api(platform(libs.jdbi.bom))
  api(platform(libs.micrometer.bom))
  api(platform(libs.opentelemetry.bom))
  api(libs.jdbi.core)
  api(libs.jdbi.kotlin)
  api(libs.jdbi.opentelemetry)
  api(libs.hikari)
  api(libs.micrometer.core)
  api(libs.kotlinx.coroutines.core)
  api(libs.kotlinx.serialization.json)
  api(libs.opentelemetry.api)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.sqlite.jdbc)
  testImplementation(libs.opentelemetry.sdk)
  testImplementation(libs.logback)
}
