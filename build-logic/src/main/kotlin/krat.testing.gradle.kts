import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
  add("testImplementation", platform(libs.findLibrary("kotest-bom").get()))
  add("testImplementation", libs.findLibrary("kotest-runner-junit5").get())
  add("testImplementation", libs.findLibrary("kotest-assertions-core").get())
}

tasks.withType<Test>().configureEach {
  useJUnitPlatform()
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  testLogging { events("PASSED", "SKIPPED", "FAILED") }
}
