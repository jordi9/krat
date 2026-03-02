# krat

Kotlin utilities for Ktor applications, inspired by [Ratpack](https://ratpack.io).

## Modules

| Module | Description |
|--------|-------------|
| krat-pack-core | Handler, Route, Service, HealthCheck, SseHandler |
| krat-pack-cors | CORS configuration |
| krat-otel | OpenTelemetry extensions |
| krat-jdbi | JDBI extensions for Loom |
| krat-time | TimeClock abstraction |
| krat-logging | KotlinLogging typealias |
| kogiven | Kotest BDD specs |

## Installation

```kotlin
repositories {
    maven("https://git.j9.io/api/packages/j9/maven")
}

dependencies {
    implementation("krat:krat-pack-core:$version")
}
```

See [Releases](https://github.com/j9/krat/releases) for versions.

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

CI publishes to Gitea and creates a GitHub release.
