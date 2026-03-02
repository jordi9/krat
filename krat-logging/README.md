# krat-logging

A simple typealias for [kotlin-logging](https://github.com/oshai/kotlin-logging).

## Installation

```kotlin
dependencies {
    implementation("krat:krat-logging:$version")
}
```

## Usage

```kotlin
import krat.logging.Logging

private val logger = Logging.logger {}

class MyService {
    fun doSomething() {
        logger.info { "Processing request" }
        logger.debug { "Details: $details" }
        logger.error(exception) { "Something went wrong" }
    }
}
```

## Why?

This module provides a consistent import across all Krat libraries. Instead of importing `io.github.oshai.kotlinlogging.KotlinLogging` directly, you import `krat.logging.Logging`.

Benefits:
- Consistent logging API across your codebase
- Easy to swap logging implementation if needed
- Clear dependency on the logging abstraction

## Testing

Use [krat-logging-testlib](../krat-logging-testlib) to capture and assert on log events in tests.

## License

MIT
