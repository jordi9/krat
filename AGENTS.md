# AGENTS.md

Krat is a Kotlin library monorepo with shared utilities for Ktor applications, published to Maven Central.

## Conventions

- Run `./gradlew spotlessApply` before committing Kotlin changes. The Git pre-commit hook (`.githooks/pre-commit`) formats staged Kotlin files; jj commits do not invoke it.
- Modules apply two convention plugins from `build-logic/`:
  - `krat.kotlin-library` — Kotlin/JVM, Spotless (ktlint), Kotest.
  - `krat.maven-publish` — vanniktech publishing + GPG signing.
- Tests use Kotest; follow the spec style of the module you are changing.

## Publishing

Tag-based to Maven Central. Tag format: `{module}/v{version}` (e.g. `krat-pack-core/v0.4.0`).

CI parses the tag, runs `./gradlew :{module}:publishAndReleaseToMavenCentral -Pversion={version}`, generates release notes with git-cliff, and creates a GitHub release.

Maven coordinates: `com.jordi9:{module}:{version}`.

## Adding a module

1. `mkdir krat-{name}` and add a `build.gradle.kts` applying `krat.kotlin-library` + `krat.maven-publish`. Set `group = "com.jordi9"` and `description = "..."` (used for the POM). Copy the shape from any existing module.
2. Add `include("krat-{name}")` to `settings.gradle.kts`.
3. Put sources under `src/main/kotlin/` and tests under `src/test/kotlin/`, following the package layout of the module (for example, `krat-kogiven` uses `com/jordi9/kogiven/`).
