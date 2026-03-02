pluginManagement {
  includeBuild("build-logic")
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "krat"

// Ratpack-inspired modules
include("krat-pack-core")
include("krat-pack-cors")

include("kogiven")
include("krat-gag")
include("krat-jdbi")
include("krat-logging")
include("krat-logging-testlib")
include("krat-otel")
include("krat-otel-canonical-traces")
include("krat-otel-testlib")
include("krat-time")
include("krat-time-testlib")
