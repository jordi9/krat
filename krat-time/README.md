# krat-time

Clean time abstraction for production code. Use `TimeClock` instead of calling `Instant.now()` directly to make your
code testable.

## Installation

```kotlin
dependencies {
    implementation("krat:krat-time:$version")
}
```

## Usage

**Production code:**

```kotlin
class VideoRepository(
    private val jdbi: Jdbi,
    private val timeClock: TimeClock
) {
    fun create(videoId: String): Video {
        val now = timeClock.now().toEpochMilli()
        // ... use now for timestamps
    }
}
```

**Wiring (Registry):**

```kotlin
class Registry(
    val timeClock: TimeClock = SystemTime  // Default to system time
) {
    fun videoRepository() = VideoRepository(jdbi(), timeClock)
}
```

## Design

- **`TimeClock` interface**: Single method `now(): Instant`
- **`SystemTime` object**: Production implementation using `Instant.now()`
- **No global state**: Each component gets its own clock instance
- **Thread-safe**: No shared mutable state

## Why not inject `Clock`?

You could inject `java.time.Clock` everywhere, but `TimeClock` is simpler:

- Single method (`now()`) instead of multiple clock operations
- Returns `Instant` directly (what you actually need)
- Cleaner API for the common case

See `krat-time-testlib` for test implementations and how to use it in tests.
