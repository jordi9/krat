# krat-time-testlib

Test implementations of `TimeClock` for controlling time in tests.

## Installation

```kotlin
dependencies {
    testImplementation("krat:krat-time-testlib:$version")
}
```

## Test Implementations

### FixedTime

Returns the same instant every time. Use for tests that need consistent timestamps.

```kotlin
val clock = FixedTime(Instant.parse("2021-01-01T00:00:00Z"))
clock.now() // Always returns 2021-01-01T00:00:00Z
```

### AdvancingTime

Returns a fixed instant that can be advanced manually. Use for tests that simulate time passing.

```kotlin
val clock = AdvancingTime(Instant.parse("2021-01-01T00:00:00Z"))

clock.now() // 2021-01-01T00:00:00Z

clock.advance(Duration.ofMinutes(5))
clock.now() // 2021-01-01T00:05:00Z

clock.advance(Duration.ofHours(1))
clock.now() // 2021-01-01T01:05:00Z
```

## Usage Patterns

### Direct Injection (Unit/Sociable Tests)

Inject the test clock directly into your components:

```kotlin
val clock = FixedTime(Instant.parse("2021-01-01T00:00:00Z"))
val repository = Repository(jdbi, clock)

val entity = repository.create("abc123")
entity.createdAt shouldBe Instant.parse("2021-01-01T00:00:00Z")
```

### Component Tests with Custom Time

Create a test application with custom time. Make sure test fixtures use the same clock:

```kotlin
"entity timestamps are consistent" {
    val clock = FixedTime(Instant.parse("2021-01-01T00:00:00Z"))

    // Create app with a custom clock
    val app = createTestApp(timeClock = clock)

    Given.`entity exists`()  // Uses fixed time
    When.`performing action`()
    Then.`timestamps are correct`()
}
```

### Advancing Time in Tests

Simulate time passing between operations:

```kotlin
"entity is marked stale after timeout" {
    val clock = AdvancingTime(Instant.parse("2021-01-01T00:00:00Z"))
    val app = createTestApp(timeClock = clock)

    Given.`entity created`()  // createdAt = 2021-01-01T00:00:00Z
    When.`checking entity status`() // calls clock.advance(Duration.ofHours(25))
    Then.`entity is marked as stale`()  // 25 hours later
}
```

## Thread Safety

This approach is fully thread-safe for parallel test execution:

- **Shared app**: Recommended to use `FixedTime` to have deterministic time – safe for parallel tests
- **Custom time**: Each test creates its own app instance with isolated `TimeClock` – safe for parallel tests
- **Within a test**: `AdvancingTime.advance()` is called sequentially between test steps – no concurrency issues

Tests using custom time are isolated from each other and can run in parallel.
