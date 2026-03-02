# krat-pack-core

Core building blocks for Ktor applications, inspired by [Ratpack](https://ratpack.io/).

## Installation

```kotlin
dependencies {
    implementation("krat:krat-pack-core:$version")
}
```

## Features

### Handler Interface

A simple interface for HTTP request handlers:

```kotlin
interface Handler {
    suspend fun handle(call: ApplicationCall)
}
```

Use with Route extensions for cleaner routing:

```kotlin
class UserHandler : Handler {
    override suspend fun handle(call: ApplicationCall) {
        val userId = call.parameters["id"]
        call.respond(HttpStatusCode.OK, "User: $userId")
    }
}

routing {
    get("/users/{id}", UserHandler())
    post("/users", CreateUserHandler())
    put("/users/{id}", UpdateUserHandler())
    delete("/users/{id}", DeleteUserHandler())
}
```

### Service Lifecycle

Services participate in the application lifecycle - started before accepting requests, stopped during shutdown:

```kotlin
class DatabaseService(private val config: DatabaseConfig) : Service {
    private lateinit var connection: Connection

    override suspend fun start() {
        connection = createConnection(config)
    }

    override suspend fun stop() {
        connection.close()
    }
}

// Install services
fun Application.module() {
    installService(DatabaseService(config))
    // or install multiple at once
    installServices(Services(listOf(service1, service2)))
}
```

### Health Checks

Built-in health check endpoints for Kubernetes-style probes:

```kotlin
routing {
    healthChecks(
        basePath = "/health",
        checks = listOf(
            HealthCheck("database") {
                if (db.isConnected()) HealthCheckResult.healthy("Connected")
                else HealthCheckResult.unhealthy("Connection lost")
            },
            HealthCheck("cache") {
                HealthCheckResult.healthy("Redis OK")
            }
        ),
        readiness = listOf(
            HealthCheck("database") { /* subset for readiness */ }
        )
    )
}
```

This creates:
- `GET /health` - All checks (returns 503 if any unhealthy)
- `GET /health/liveness` - Always returns 200 OK
- `GET /health/readiness` - Readiness checks only
- `GET /health/{name}` - Individual check by name

### SSE Handler

Handler for Server-Sent Events streaming:

```kotlin
class NotificationHandler : SseHandler {
    override suspend fun events(call: ApplicationCall): Flow<ServerSentEvent> = flow {
        while (true) {
            emit(ServerSentEvent(data = "heartbeat"))
            delay(30.seconds)
        }
    }
}

routing {
    sse("/notifications", NotificationHandler())
}
```

### Config Utility

Type-safe configuration access:

```kotlin
val dbUrl: String = config("database.url")
val port: Int = config("server.port")
```

## Examples

See [HealthCheckTest.kt](src/test/kotlin/krat/pack/core/HealthCheckTest.kt) for comprehensive usage examples.
