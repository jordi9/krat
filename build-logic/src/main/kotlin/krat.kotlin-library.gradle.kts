plugins {
  kotlin("jvm")
  id("com.diffplug.spotless")
}

spotless {
  ratchetFrom("origin/main")
  kotlin {
    ktlint("1.8.0")
  }
}

repositories {
  mavenCentral()
}

kotlin {
  jvmToolchain(24)
}

tasks.register("ktlintFormat") {
  dependsOn("spotlessApply")
}

tasks.register("ktlintCheck") {
  dependsOn("spotlessCheck")
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging { events("PASSED", "SKIPPED", "FAILED") }
}
