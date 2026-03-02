# CLAUDE.md

Krat: Kotlin library monorepo with shared utilities for Ktor applications, published to Maven Central.

## Build Commands

```bash
./gradlew ktlintFormat   # Format code (always run first)
./gradlew build          # Build all modules
./gradlew test           # Run all tests
./gradlew :module:test   # Run tests for specific module
```

## Module Structure

### Ratpack-inspired (`com.jordi9.krat.pack.*`)

| Module | Package | Description |
|--------|---------|-------------|
| krat-pack-core | `com.jordi9.krat.pack.core` | Handler, Route, Service, HealthCheck, SseHandler, Config |
| krat-pack-cors | `com.jordi9.krat.pack.cors` | CorsConfig, installCors |

### OpenTelemetry (`com.jordi9.krat.otel.*`)

| Module | Package | Description |
|--------|---------|-------------|
| krat-otel | `com.jordi9.krat.otel` | OpenTelemetryProvider, TracerExtensions, SpanExtensions, NoiseSampler |
| krat-otel-canonical-traces | `com.jordi9.krat.otel.canonicaltraces` | LoggingSpanProcessor (canonical log lines) |

### Utilities (`com.jordi9.krat.*`)

| Module | Package | Description |
|--------|---------|-------------|
| krat-logging | `com.jordi9.krat.logging` | KotlinLogging typealias |
| krat-time | `com.jordi9.krat.time` | TimeClock interface, SystemTime |
| krat-gag | `com.jordi9.krat.gag` | YOLO annotation |
| krat-jdbi | `com.jordi9.krat.jdbi` | JdbiProvider, JdbiExtensions (Loom), DatabaseConfig |

### Testing

| Module | Package | Description |
|--------|---------|-------------|
| kogiven | `com.jordi9.kogiven` | Kotest BDD (ScenarioStringSpec, ScenarioFunSpec) |
| krat-otel-testlib | `com.jordi9.krat.otel.testlib` | OpenTelemetryTestProvider for in-memory span capture |
| krat-logging-testlib | `com.jordi9.krat.logging` | LogEventsExtension for Kotest |
| krat-time-testlib | `com.jordi9.krat.time` | FixedTime, AdvancingTime |

## Publishing

Tag-based publishing to Maven Central. Tag format: `{module}/v{version}`

```bash
# Example: publish krat-pack-core version 0.4.0
git tag krat-pack-core/v0.4.0
git push origin krat-pack-core/v0.4.0
```

CI automatically:
1. Parses tag to extract module and version
2. Runs `./gradlew :{module}:publishAndReleaseToMavenCentral -Pversion={version}`
3. Generates release notes with git-cliff
4. Creates GitHub release

Maven coordinates: `com.jordi9:{module}:{version}`

## Convention Plugins

All modules use convention plugins from `build-logic/`:

- `krat.kotlin-library` - Kotlin/JVM setup, Java 24, ktlint, Kotest
- `krat.maven-publish` - Publish to Maven Central (vanniktech/maven-publish, GPG signing)

## Tech Stack

- Kotlin 2.3.0, Java 25
- Ktor 3.3.2
- Kotest 6.0.5
- JDBI 3.49.5
- OpenTelemetry 1.57.0
- Micrometer 1.15.0

## Adding a New Module

1. Create directory: `krat-{name}/`
2. Add `build.gradle.kts`:
   ```kotlin
   plugins {
     id("krat.kotlin-library")
     id("krat.maven-publish")
   }

   group = "com.jordi9"
   description = "Short description for Maven POM"

   dependencies {
     // your dependencies
   }
   ```
3. Add to `settings.gradle.kts`: `include("krat-{name}")`
4. Create source: `src/main/kotlin/com/jordi9/krat/{name}/`
5. Create tests: `src/test/kotlin/com/jordi9/krat/{name}/`
