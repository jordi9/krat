plugins {
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "build-logic"

includeBuild("../krat-plugin-ktlint")

dependencyResolutionManagement {
  repositories {
    gradlePluginPortal()
    mavenCentral()
  }
  versionCatalogs {
    create("libs") {
      from(files("../gradle/libs.versions.toml"))
    }
  }
}
