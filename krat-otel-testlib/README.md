# krat-otel-testlib

In-memory span capture for testing OpenTelemetry instrumentation.

## Installation

```kotlin
dependencies {
    testImplementation("krat:krat-otel-testlib:$version")
}
```

## Usage

```kotlin
class MyServiceTest : StringSpec({

    val otelTest = OpenTelemetryTestProvider(
        OpenTelemetryConfig(serviceName = "test-service")
    )

    val tracer = otelTest.provider.get().getTracer("test")

    afterEach {
        otelTest.reset()
    }

    afterSpec {
        otelTest.close()
    }

    "should create span for operation" {
        tracer.withSpan("my-operation") {
            setAppAttribute("key", "value")
        }

        otelTest.finishedSpans shouldHaveSize 1
        otelTest.finishedSpans.first().name shouldBe "my-operation"
    }

    "should record errors" {
        shouldThrow<RuntimeException> {
            tracer.withSpan("failing-operation") {
                throw RuntimeException("boom")
            }
        }

        val span = otelTest.finishedSpans.first()
        span.status.statusCode shouldBe StatusCode.ERROR
    }
})
```

## API

### OpenTelemetryTestProvider

| Property/Method | Description |
|-----------------|-------------|
| `provider` | The underlying `OpenTelemetryProvider` instance |
| `spanExporter` | Access to the `InMemorySpanExporter` |
| `finishedSpans` | List of all captured `SpanData` |
| `reset()` | Clear all captured spans |
| `close()` | Clean up resources |

## Examples

See [OpenTelemetryTestProviderShould.kt](src/test/kotlin/krat/otel/testlib/OpenTelemetryTestProviderShould.kt).
