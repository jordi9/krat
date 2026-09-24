plugins {
  id("krat.kotlin-library")
  id("krat.maven-publish")
  alias(libs.plugins.kotlin.serialization)
}

group = "com.jordi9"
description = "Flyway migration runner and pending migration inspection for JDBC databases"

dependencies {
  api(libs.flyway.core)
  api(libs.kotlinx.serialization.json)

  testImplementation(libs.sqlite.jdbc)
}
