# krat

A Kotlin library monorepo — small, focused utilities for backend applications. The `krat-pack-*` modules bring [Ratpack](https://ratpack.io)-inspired patterns to [Ktor](https://ktor.io). The rest of the pack works anywhere.

## The Rat Pack

Ktor primitives inspired by the elegance of Ratpack.

| Module | Description |
|--------|-------------|
| [krat-pack-core](krat-pack-core/) | Handler, Route, Service, HealthCheck, SseHandler |
| [krat-pack-cors](krat-pack-cors/) | CORS configuration for Ktor |

## Observability

OpenTelemetry utilities — no Ktor dependency required.

| Module | Description |
|--------|-------------|
| [krat-otel](krat-otel/) | OpenTelemetry provider, tracer/span extensions, NoiseSampler |
| [krat-otel-canonical-traces](krat-otel-canonical-traces/) | Canonical trace log line processor |
| [krat-otel-testlib](krat-otel-testlib/) | In-memory span capture for testing |

## Utilities

Framework-agnostic building blocks.

| Module | Description |
|--------|-------------|
| [krat-jdbi](krat-jdbi/) | JDBI provider with HikariCP, Micrometer, and Loom support |
| [krat-flyway](krat-flyway/) | Explicit Flyway migrations and pending migration inspection |
| [krat-logging](krat-logging/) | KotlinLogging typealias |
| [krat-time](krat-time/) | TimeClock interface for testable time access |
| [krat-gag](krat-gag/) | YOLO annotation |

## Testing

| Module | Description |
|--------|-------------|
| [krat-kogiven](krat-kogiven/) | Kotest BDD scenario specs |
| [krat-logging-testlib](krat-logging-testlib/) | Log event capture for Kotest |
| [krat-time-testlib](krat-time-testlib/) | Fixed and advancing time implementations |

## Getting Started

Check out [krat-skeleton](https://github.com/jordi9/krat-skeleton) — a Kotlin REST API template using Ktor and hexagonal architecture that demonstrates how to use krat modules together.

## Installation

All modules are published independently to Maven Central as `com.jordi9:{module}:{version}`.

```kotlin
dependencies {
    implementation("com.jordi9:krat-pack-core:$version")
    implementation("com.jordi9:krat-otel:$version")
    testImplementation("com.jordi9:krat-kogiven:$version")
}
```

See [Releases](https://github.com/jordi9/krat/releases) for versions.

### JitPack

JitPack can also build the repository from a commit or tag. Look up
[`com.jordi9/krat`](https://jitpack.io/#com.jordi9/krat) and use the module coordinates
shown by **Get it**. The custom domain changes the group prefix, not the download
host; individual modules use the `com.jordi9.krat` group rather than Central's
`com.jordi9`.

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io") {
        content { includeGroup("com.jordi9.krat") }
    }
}

dependencies {
    implementation("com.jordi9.krat:krat-pack-core:$jitpackVersion")
}
```

Use a commit hash for `jitpackVersion` to try a build without creating a release.
JitPack builds all modules at that revision with one version; this is separate
from Central's independent module releases. Its build compiles and packages the
libraries without compiling or running tests, then publishes to Maven Local for
JitPack to collect, without requiring Central or GPG credentials.

## Releasing

### Using release.sh (Recommended)

Interactive script for releasing modules:

```bash
./release.sh              # Single module release
./release.sh --batch      # Select multiple modules
./release.sh --all patch  # Bump all modules
./release.sh --dry-run    # Preview without executing
```

### Manual Release

Tag format: `{module}/v{version}`

```bash
git tag krat-pack-core/v0.4.0
git push origin krat-pack-core/v0.4.0
```

CI publishes to Maven Central and creates a GitHub release.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
