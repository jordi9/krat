plugins {
  kotlin("jvm")
  id("krat.ktlint")
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
