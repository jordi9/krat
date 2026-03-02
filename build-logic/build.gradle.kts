plugins {
  `kotlin-dsl`
}

kotlin {
  jvmToolchain(24)
}

dependencies {
  implementation(libs.plugins.kotlin.jvm.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
  implementation(libs.plugins.vanniktech.publish.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
  implementation("com.jordi9:krat-plugin-ktlint")
}
