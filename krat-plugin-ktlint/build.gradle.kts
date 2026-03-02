plugins {
  `java-gradle-plugin`
  `maven-publish`
  kotlin("jvm") version "2.3.10"
}

group = "com.jordi9"

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(24)
}

// Version from -Pversion (CI sets this from git tag) or default snapshot
version = findProperty("version")?.toString()?.takeIf { it != "unspecified" } ?: "0.0.0-SNAPSHOT"

gradlePlugin {
  plugins {
    create("ktlint") {
      id = "krat.ktlint"
      implementationClass = "com.jordi9.krat.plugin.ktlint.KtlintPlugin"
    }
  }
}

dependencies {
  testImplementation(gradleTestKit())
  testImplementation("io.kotest:kotest-runner-junit5:6.1.4")
  testImplementation("io.kotest:kotest-assertions-core:6.1.4")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
}

publishing {
  // java-gradle-plugin automatically creates 'pluginMaven' publication
  // This plugin is consumed via includeBuild by build-logic, not published externally
}
