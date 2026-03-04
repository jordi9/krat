plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Ktor test utilities for HTTP response assertions"

dependencies {
  api(platform(libs.ktor.bom))
  api(libs.ktor.client.core)
  api(libs.kotlinx.serialization.json)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
}
