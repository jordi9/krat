plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "Kotest BDD extensions for scenario-based testing"

dependencies {
  implementation(platform(libs.kotest.bom))
  implementation(libs.kotest.runner.junit5)

  testImplementation(libs.kotest.assertions.core)
}
