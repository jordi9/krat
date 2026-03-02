# krat-logging-testlib

Kotest extension for capturing and asserting log events in tests.

## Installation

```kotlin
dependencies {
  testImplementation("krat:krat-logging-testlib:$kratVersion")
}
```

## Usage

### Basic Usage

Capture all log events from the root logger:

```kotlin
class MyTest : StringSpec({

  val logs = extension(LogEventsExtension())

  "should log a message" {
    myService.doSomething()

    logs.events shouldHaveSize 1
    logs.events.first().message shouldBe "Expected message"
  }

  "logs are cleared between tests" {
    // Previous test's logs are gone
    logs.events.shouldBeEmpty()
  }
})
```

### Scoped to Specific Logger

Capture events only from a specific logger:

```kotlin
class MyServiceTest : StringSpec({

  val logs = extension(LogEventsExtension(LoggerFactory.getLogger(MyService::class.java)))

  "captures only MyService logs" {
    myService.doSomething()        // logs to MyService logger
    otherService.doSomething()     // logs to OtherService logger - ignored

    logs.events shouldHaveSize 1
  }
})
```

### Asserting Log Properties

The captured events are Logback `ILoggingEvent` objects with full access to:

```kotlin
"can assert on log properties" {
  logger.warn("User {} failed login", "alice")

  val event = logs.events.last()
  event.level.toString() shouldBe "WARN"
  event.message shouldBe "User {} failed login"
  event.formattedMessage shouldBe "User alice failed login"
  event.loggerName shouldBe "com.example.MyClass"
}
```

### Asserting MDC Values

For structured logging with MDC:

```kotlin
"can assert on MDC values" {
  MDC.put("correlationId", "abc123")
  logger.info("Processing request")
  MDC.clear()

  logs.events.last().mdcPropertyMap shouldContainAll mapOf(
    "correlationId" to "abc123"
  )
}
```

## How It Works

`LogEventsExtension` implements Kotest's `TestListener` interface:

- **beforeTest**: Creates a fresh `ListAppender` and attaches it to the logger
- **afterTest**: Detaches the appender, ensuring clean isolation between tests

This means each test starts with an empty `events` list, preventing test pollution.
