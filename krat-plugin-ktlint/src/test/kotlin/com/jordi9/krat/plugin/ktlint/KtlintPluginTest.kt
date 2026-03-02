package com.jordi9.krat.plugin.ktlint

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

class KtlintPluginTest : FunSpec({

  lateinit var projectDir: File

  beforeTest {
    projectDir = createTempDirectory("ktlint-plugin-test").toFile()
  }

  afterTest {
    projectDir.deleteRecursively()
  }

  fun buildFile(extraConfig: String = "") = """
        plugins {
            id("krat.ktlint")
        }

        repositories {
            mavenCentral()
        }
        $extraConfig
    """.trimIndent()

  test("registers ktlintCheck and ktlintFormat tasks") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("tasks", "--group=verification")
      .build()

    result.output shouldContain "ktlintCheck"
  }

  test("ktlintCheck passes on well-formatted code") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve("src/main/kotlin").mkdirs()
    projectDir.resolve("src/main/kotlin/Example.kt").writeText(
      """
            package example

            fun hello() = "world"
            """.trimIndent() + "\n"
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintCheck")
      .build()

    result.task(":ktlintCheck")?.outcome shouldBe TaskOutcome.SUCCESS
  }

  test("ktlintCheck fails on poorly formatted code") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve("src/main/kotlin").mkdirs()
    projectDir.resolve("src/main/kotlin/Example.kt").writeText(
      """
            package example
            fun hello()="world"
            """.trimIndent()
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintCheck")
      .buildAndFail()

    result.task(":ktlintCheck")?.outcome shouldBe TaskOutcome.FAILED
  }

  test("ktlintFormat fixes formatting issues") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve("src/main/kotlin").mkdirs()
    val sourceFile = projectDir.resolve("src/main/kotlin/Example.kt")
    sourceFile.writeText(
      """
            package example
            fun hello()="world"
            """.trimIndent()
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintFormat")
      .build()

    result.task(":ktlintFormat")?.outcome shouldBe TaskOutcome.SUCCESS
    sourceFile.readText() shouldContain "fun hello() = \"world\""
  }

  test("ktlintCheck hooks into check task when base plugin applied") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                base
                id("krat.ktlint")
            }

            repositories {
                mavenCentral()
            }
            """.trimIndent()
    )
    projectDir.resolve("src/main/kotlin").mkdirs()
    projectDir.resolve("src/main/kotlin/Example.kt").writeText(
      """
            package example

            fun hello() = "world"
            """.trimIndent() + "\n"
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("check", "--dry-run")
      .build()

    result.output shouldContain ":ktlintCheck SKIPPED"
  }

  test("version is configurable via extension") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(
      """
            plugins {
                id("krat.ktlint")
            }

            repositories {
                mavenCentral()
            }

            ktlint {
                version.set("1.5.0")
            }
            """.trimIndent()
    )
    projectDir.resolve("src/main/kotlin").mkdirs()
    projectDir.resolve("src/main/kotlin/Example.kt").writeText(
      """
            package example

            fun hello() = "world"
            """.trimIndent() + "\n"
    )

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintCheck", "--info")
      .build()

    result.task(":ktlintCheck")?.outcome shouldBe TaskOutcome.SUCCESS
    result.output shouldContain "ktlint-cli-1.5.0"
  }

  test("installKtlintHook creates pre-commit hook") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve(".git/hooks").mkdirs()

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("installKtlintHook")
      .build()

    result.task(":installKtlintHook")?.outcome shouldBe TaskOutcome.SUCCESS
    val hookFile = projectDir.resolve(".git/hooks/pre-commit")
    hookFile.exists() shouldBe true
    hookFile.readText() shouldContain "KTLINT HOOK"
    hookFile.canExecute() shouldBe true
  }

  test("installKtlintHook is idempotent") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve(".git/hooks").mkdirs()

    // Run twice
    GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("installKtlintHook")
      .build()

    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("installKtlintHook")
      .build()

    result.task(":installKtlintHook")?.outcome shouldBe TaskOutcome.SUCCESS
    result.output shouldContain "already installed"
  }

  test("ktlintCheck is cacheable") {
    projectDir.resolve("settings.gradle.kts").writeText("")
    projectDir.resolve("build.gradle.kts").writeText(buildFile())
    projectDir.resolve("src/main/kotlin").mkdirs()
    projectDir.resolve("src/main/kotlin/Example.kt").writeText(
      """
            package example

            fun hello() = "world"
            """.trimIndent() + "\n"
    )

    // First run
    GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintCheck")
      .build()

    // Second run should be up-to-date
    val result = GradleRunner.create()
      .withProjectDir(projectDir)
      .withPluginClasspath()
      .withArguments("ktlintCheck")
      .build()

    result.task(":ktlintCheck")?.outcome shouldBe TaskOutcome.UP_TO_DATE
  }
})
