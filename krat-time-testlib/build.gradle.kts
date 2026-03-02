plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Fixed and advancing time implementations for testing"

dependencies {
  implementation(project(":krat-time"))

  implementation(platform(libs.kotest.bom))
  implementation(libs.kotest.runner.junit5)

  testImplementation(libs.kotest.assertions.core)
  testImplementation(libs.kotlinx.coroutines.core)
}
