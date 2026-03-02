# krat-plugin-ktlint

Gradle plugin for [ktlint](https://pinterest.github.io/ktlint/) that uses `JavaExec` to run ktlint-cli directly,
allowing custom JVM arguments.

## Why?

Neither [ktlint-gradle](https://github.com/JLLeitschuh/ktlint-gradle)
nor [kotlinter-gradle](https://github.com/jeremymailen/kotlinter-gradle) support passing JVM args to their worker
processes. This causes `sun.misc.Unsafe` warnings on Java 23+.

This plugin runs ktlint-cli via `JavaExec` with `--sun-misc-unsafe-memory-access=allow` to suppress those warnings.

## Installation

Add the plugin to your build:

```kotlin
// settings.gradle.kts
pluginManagement {
  repositories {
    maven("https://gitea.cod-tyrannosaurus.ts.net/api/packages/homelab/maven")
    gradlePluginPortal()
  }
}

// build.gradle.kts
plugins {
  id("krat.ktlint") version "0.1.0"
}
```

## Configuration

```kotlin
ktlint {
  version.set("1.8.0")  // ktlint-cli version (default: 1.8.0)
}
```

## Tasks

| Task                | Description                                   |
|---------------------|-----------------------------------------------|
| `ktlintCheck`       | Check Kotlin code style. Fails on violations. |
| `ktlintFormat`      | Fix Kotlin code style violations.             |
| `installKtlintHook` | Install git pre-commit hook for ktlintFormat. |

When the `base` plugin is applied, `ktlintCheck` automatically runs as part of the `check` task.

## Git Hook

Install a pre-commit hook that auto-formats staged Kotlin files:

```bash
./gradlew :some-module:installKtlintHook
```

Run from any module that applies the plugin - the hook is installed at the repo root.

The hook will:
1. Run `ktlintFormat` on staged `.kt`/`.kts` files
2. Re-stage the formatted files
3. Preserve unstaged changes

## Reports

HTML reports are generated at `build/reports/ktlint/ktlint.html`.

## Example

```kotlin
plugins {
  kotlin("jvm") version "2.3.0"
  id("krat.ktlint") version "1.0.0"
}

repositories {
  mavenCentral()
}

ktlint {
  version.set("1.5.0")
}
```

```bash
./gradlew ktlintCheck    # Check formatting
./gradlew ktlintFormat   # Auto-fix formatting
./gradlew check          # Runs ktlintCheck with other checks
```
