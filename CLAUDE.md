# CLAUDE.md

Krat is a Kotlin library monorepo with shared utilities for Ktor applications, published to Maven Central.

## Conventions

- Always `./gradlew spotlessApply` before committing — a pre-commit hook enforces it (`.githooks/pre-commit`).
- Modules apply two convention plugins from `build-logic/`:
  - `krat.kotlin-library` — Kotlin/JVM, Spotless (ktlint), Kotest.
  - `krat.maven-publish` — vanniktech publishing + GPG signing.
- Tests use Kotest `StringSpec`.

## Publishing

Tag-based to Maven Central. Tag format: `{module}/v{version}` (e.g. `krat-pack-core/v0.4.0`).

CI parses the tag, runs `./gradlew :{module}:publishAndReleaseToMavenCentral -Pversion={version}`, generates release notes with git-cliff, and creates a GitHub release.

Maven coordinates: `com.jordi9:{module}:{version}`.

## Adding a module

1. `mkdir krat-{name}` and add a `build.gradle.kts` applying `krat.kotlin-library` + `krat.maven-publish`. Set `group = "com.jordi9"` and `description = "..."` (used for the POM). Copy the shape from any existing module.
2. Add `include("krat-{name}")` to `settings.gradle.kts`.
3. Sources go in `src/main/kotlin/com/jordi9/krat/{name}/`, tests mirror under `src/test/...`.
