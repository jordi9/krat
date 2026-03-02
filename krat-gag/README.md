# krat-gag

A `@YOLO` annotation for marking code that deliberately breaks clean code rules.

## Installation

```kotlin
dependencies {
    implementation("krat:krat-gag:$version")
}
```

## Usage

Sometimes you need to write code that violates best practices for pragmatic reasons. The `@YOLO` annotation lets you document these decisions:

```kotlin
import krat.gag.YOLO

@YOLO("Temporary workaround until API v2 is released")
fun fetchDataWithRetry(): Data {
    // Code that's not ideal but works for now
}

@YOLO("Performance optimization - inlined for hot path")
class InlinedValidator {
    // Violates SRP but measurably faster
}

@YOLO("Legacy integration requires this exact format")
val MAGIC_HEADER = "X-Legacy-Auth-Token-v1"
```

## Why?

Clean code rules exist for good reasons, but real-world software sometimes requires trade-offs. The `@YOLO` annotation:

1. **Documents intent** - Makes it clear the violation is deliberate, not accidental
2. **Explains reasoning** - The `reason` parameter captures why the trade-off was made
3. **Enables search** - Find all such code with a simple search for `@YOLO`
4. **Source-only** - Retained only at source level, no runtime overhead

## Annotation Targets

Can be applied to:
- Classes
- Functions
- Properties
- Constructors
- Type aliases
- Fields
- Expressions
- Local variables
- Value parameters
- Types
