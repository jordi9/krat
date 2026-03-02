package com.jordi9.krat.plugin.ktlint

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.attributes.Bundling
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Gradle plugin for ktlint using JavaExec.
 *
 * Uses JavaExec to run ktlint-cli directly, allowing JVM args to suppress
 * sun.misc.Unsafe warnings on Java 23+. Neither ktlint-gradle nor kotlinter-gradle
 * support passing JVM args to their workers.
 *
 * Configuration:
 * ```
 * ktlint {
 *     version.set("1.8.0")
 * }
 * ```
 */
class KtlintPlugin : Plugin<Project> {

  companion object {
    const val DEFAULT_KTLINT_VERSION = "1.8.0"
    const val CONFIGURATION_NAME = "ktlintCli"
    const val EXTENSION_NAME = "ktlint"
  }

  override fun apply(project: Project) {
    val extension = project.extensions.create(EXTENSION_NAME, KtlintExtension::class.java)
    extension.version.convention(DEFAULT_KTLINT_VERSION)

    val ktlintCli = createKtlintConfiguration(project, extension)
    registerKtlintCheckTask(project, ktlintCli)
    registerKtlintFormatTask(project, ktlintCli)
    registerInstallHookTask(project)
    hookIntoCheckTask(project)
  }

  private fun createKtlintConfiguration(project: Project, extension: KtlintExtension): Configuration {
    val ktlintCli = project.configurations.create(CONFIGURATION_NAME)
    ktlintCli.isCanBeResolved = true
    ktlintCli.isCanBeConsumed = false

    project.dependencies.addProvider(
      CONFIGURATION_NAME,
      extension.version.map { version ->
        val dependency = project.dependencies.create("com.pinterest.ktlint:ktlint-cli:$version")
        (dependency as ExternalModuleDependency).attributes { attrs ->
          attrs.attribute(
            Bundling.BUNDLING_ATTRIBUTE,
            project.objects.named(Bundling::class.java, Bundling.EXTERNAL)
          )
        }
        dependency
      }
    )

    return ktlintCli
  }

  private fun registerKtlintCheckTask(project: Project, ktlintCli: Configuration) {
    val ktlintInputFiles = project.fileTree(mapOf("dir" to "src", "include" to listOf("**/*.kt", "**/*.kts")))
    val ktlintReportsDir = project.layout.buildDirectory.dir("reports/ktlint")

    project.tasks.register("ktlintCheck", JavaExec::class.java) { task ->
      task.group = "verification"
      task.description = "Check Kotlin code style"
      task.classpath = ktlintCli
      task.mainClass.set("com.pinterest.ktlint.Main")
      task.jvmArgs("--sun-misc-unsafe-memory-access=allow")
      task.args(
        "--log-level=error",
        "--reporter=plain",
        "--reporter=html,output=${ktlintReportsDir.get()}/ktlint.html",
        "src/**/*.kt",
        "src/**/*.kts"
      )

      task.inputs.files(ktlintInputFiles).withPathSensitivity(PathSensitivity.RELATIVE)
      task.inputs.files(ktlintCli).withPathSensitivity(PathSensitivity.RELATIVE)
      task.outputs.dir(ktlintReportsDir)
    }
  }

  private fun registerKtlintFormatTask(project: Project, ktlintCli: Configuration) {
    project.tasks.register("ktlintFormat", JavaExec::class.java) { task ->
      task.group = "formatting"
      task.description = "Fix Kotlin code style"
      task.classpath = ktlintCli
      task.mainClass.set("com.pinterest.ktlint.Main")
      task.jvmArgs("--sun-misc-unsafe-memory-access=allow")
      task.args("--log-level=error", "-F", "src/**/*.kt", "src/**/*.kts")
    }
  }

  private fun registerInstallHookTask(project: Project) {
    project.tasks.register("installKtlintHook", InstallKtlintHookTask::class.java) { task ->
      task.group = "formatting"
      task.description = "Install git pre-commit hook for ktlintFormat"
      task.rootDir.set(project.rootDir)
    }
  }

  private fun hookIntoCheckTask(project: Project) {
    project.plugins.withType(BasePlugin::class.java) {
      project.tasks.named("check").configure { it.dependsOn("ktlintCheck") }
    }
  }
}

abstract class InstallKtlintHookTask : DefaultTask() {

  @get:InputDirectory
  abstract val rootDir: DirectoryProperty

  @TaskAction
  fun install() {
    val gitDir = rootDir.get().asFile.resolve(".git")
    if (!gitDir.exists()) {
      logger.warn("No .git directory found. Skipping hook installation.")
      return
    }

    val hooksDir = gitDir.resolve("hooks")
    hooksDir.mkdirs()

    val hookFile = hooksDir.resolve("pre-commit")
    val existingContent = if (hookFile.exists()) hookFile.readText() else ""

    if (existingContent.contains("KTLINT HOOK")) {
      logger.lifecycle("ktlint hook already installed.")
      return
    }

    val hookScript = """
      |#!/bin/sh
      |######## KTLINT HOOK START ########
      |set +e
      |
      |CHANGED_FILES="${'$'}(git --no-pager diff --name-status --no-color --cached | awk '${'$'}1 != "D" && ${'$'}NF ~ /\.kts?${'$'}/ { print ${'$'}NF }')"
      |
      |if [ -z "${'$'}CHANGED_FILES" ]; then
      |  exit 0
      |fi
      |
      |echo "Running ktlint over staged files..."
      |
      |# Stash unstaged changes
      |diff=.git/unstaged-ktlint.diff
      |git diff --binary --color=never > ${'$'}diff
      |if [ -s ${'$'}diff ]; then
      |  git apply -R ${'$'}diff
      |fi
      |
      |# Run ktlint
      |./gradlew ktlintFormat --quiet
      |exit_code=${'$'}?
      |
      |# Re-add formatted files
      |echo "${'$'}CHANGED_FILES" | while read -r file; do
      |  if [ -f "${'$'}file" ]; then
      |    git add "${'$'}file"
      |  fi
      |done
      |
      |# Restore unstaged changes
      |if [ -s ${'$'}diff ]; then
      |  git apply --ignore-whitespace ${'$'}diff
      |fi
      |rm -f ${'$'}diff
      |
      |exit ${'$'}exit_code
      |######## KTLINT HOOK END ########
    """.trimMargin()

    val newContent = if (existingContent.isNotEmpty()) {
      existingContent.trimEnd() + "\n\n" + hookScript
    } else {
      hookScript
    }

    hookFile.writeText(newContent)
    hookFile.setExecutable(true)

    logger.lifecycle("Installed ktlint pre-commit hook to ${hookFile.absolutePath}")
  }
}
