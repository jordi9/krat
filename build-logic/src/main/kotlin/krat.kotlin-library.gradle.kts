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

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging { events("PASSED", "SKIPPED", "FAILED") }
}
