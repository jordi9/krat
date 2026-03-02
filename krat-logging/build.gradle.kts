plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
}

group = "com.jordi9"
description = "KotlinLogging typealias for structured logging"

dependencies {
  api(libs.kotlin.logging)
  implementation(libs.slf4j.api)
}
