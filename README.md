# krat

Kotlin utilities for Ktor applications, inspired by [Ratpack](https://ratpack.io).

## Modules

| Module | Description |
|--------|-------------|
| [krat-pack-core](krat-pack-core/) | Handler, Route, Service, HealthCheck, SseHandler |
| [krat-pack-cors](krat-pack-cors/) | CORS configuration |
| [krat-otel](krat-otel/) | OpenTelemetry provider, tracer/span extensions, NoiseSampler |
| [krat-otel-canonical-traces](krat-otel-canonical-traces/) | Canonical trace log line processor |
| [krat-otel-testlib](krat-otel-testlib/) | In-memory span capture for testing |
| [krat-jdbi](krat-jdbi/) | JDBI provider with HikariCP and Loom support |
| [krat-time](krat-time/) | TimeClock abstraction |
| [krat-time-testlib](krat-time-testlib/) | Fixed and advancing time for testing |
| [krat-logging](krat-logging/) | KotlinLogging typealias |
| [krat-logging-testlib](krat-logging-testlib/) | Log event capture for Kotest |
| [krat-gag](krat-gag/) | YOLO annotation |
| [krat-kogiven](krat-kogiven/) | Kotest BDD scenario specs |

## Installation

```kotlin
dependencies {
    implementation("com.jordi9:krat-pack-core:$version")
}
```

See [Releases](https://github.com/jordi9/krat/releases) for versions.

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
