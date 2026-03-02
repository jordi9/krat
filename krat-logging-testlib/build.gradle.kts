plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Kotest extension for capturing log events in tests"

dependencies {
  implementation(platform(libs.kotest.bom))
  implementation(libs.kotest.runner.junit5)
  implementation(libs.logback)

  implementation(project(":krat-logging"))

  testImplementation(libs.kotest.assertions.core)
}
