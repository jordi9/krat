plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
  alias(libs.plugins.kotlin.serialization)
}

group = "com.jordi9"
description = "CORS configuration for Ktor applications"

dependencies {
  api(platform(libs.ktor.bom))
  api(libs.ktor.server.cors)

  implementation(libs.kotlinx.serialization.json)

  testImplementation(platform(libs.kotest.bom))
  testImplementation(libs.kotest.runner.junit5)
  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.ktor.server.test.host)
}
