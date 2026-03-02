# krat-otel

OpenTelemetry integration for Kotlin applications with ergonomic span creation and structured logging.

## Installation

```kotlin
dependencies {
    implementation("krat:krat-otel:$version")
}
```

## Features

### OpenTelemetryProvider

Initialize OpenTelemetry with sensible defaults:

```kotlin
val otelProvider = OpenTelemetryProvider(
    OpenTelemetryConfig(
        serviceName = "my-service",
        otlpEndpoint = "http://localhost:4317",
        otlpEnabled = true,
        logFormat = LogFormat.PRETTY  // or LOGFMT for production
    )
)

val tracer = otelProvider.get().getTracer("my-service")
```

Log formats:
- `PRETTY` - Multi-line with child spans, ideal for development
- `LOGFMT` - Single-line key=value format for log aggregation
- `NONE` - Disable trace logging

### Tracer Extensions

Execute code within traced spans with automatic error handling:

```kotlin
// Synchronous span
tracer.withSpan("fetch.user", { setAttribute("user.id", userId) }) {
    val user = userRepository.findById(userId)
    setAppAttribute("user.name", user.name)
    user
}

// Suspend span for background workers (CONSUMER kind)
tracer.withWorkerSpan("process.batch") {
    items.forEach { processItem(it) }
    setAppAttribute("items.processed", items.size)
}
```

Features:
- Automatic span lifecycle management (start/end)
- Error status and exception recording on failure
- Coroutine context propagation for suspend functions

### Span Extensions

Add app-namespaced attributes (prefixed with `app.`):

```kotlin
span.setAppAttribute("order.id", orderId)        // app.order.id
span.setAppAttribute("items.count", 5)           // app.items.count
span.setAppAttribute("premium.user", true)       // app.premium.user
```

Access the current span safely:

```kotlin
withCurrentSpan {
    setAppAttribute("result.status", "success")
}
```

### LoggingSpanProcessor

Automatic trace logging when SERVER or CONSUMER spans complete:

**PRETTY format (development):**
```
  ✓ [GET /users/123] (200) 45ms | abc123def456
    user.id=123 user.type=premium
    ↳ database.query 12ms table=users
    ↳ cache.lookup 3ms hit=true
```

**LOGFMT format (production):**
```
method=GET route=/users/123 status=200 duration=45ms trace_id=abc123def456 user.id=123 span.database.query=12ms
```

## Usage with Ktor

```kotlin
fun Application.module() {
    val otelProvider = OpenTelemetryProvider(config)
    val tracer = otelProvider.get().getTracer("my-app")

    // Install Ktor OpenTelemetry plugin for automatic HTTP tracing
    install(OpenTelemetry) {
        setOpenTelemetry(otelProvider.get())
    }

    routing {
        get("/users/{id}") {
            tracer.withSpan("handler.getUser") {
                // Your code here
            }
        }
    }
}
```

## Testing

Use [krat-otel-testlib](../krat-otel-testlib) for in-memory span capture in tests.

## Examples

See [OpenTelemetryProviderTest.kt](src/test/kotlin/krat/otel/OpenTelemetryProviderTest.kt) and [LoggingSpanProcessorTest.kt](src/test/kotlin/krat/otel/LoggingSpanProcessorTest.kt).

## License

MIT
