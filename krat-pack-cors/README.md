# krat-pack-cors

Opinionated CORS configuration for Ktor applications.

## Installation

```kotlin
dependencies {
    implementation("krat:krat-pack-cors:$version")
}
```

## Usage

```kotlin
fun Application.module() {
    installCors(CorsConfig(
        allowedHosts = listOf("example.com", "api.example.com"),
        allowAnyLocalhost = true  // Allow localhost:* for development
    ))
}
```

The `allowAnyLocalhost` option allows any `localhost:*` origin while rejecting malicious lookalikes like `localhost.evil.com`.
