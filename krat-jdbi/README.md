# krat-jdbi

JDBI integration with HikariCP connection pooling and virtual thread (Loom) support.

## Installation

```kotlin
dependencies {
    implementation("krat:krat-jdbi:$version")
}
```

## Features

### JdbiProvider

Creates a configured JDBI instance with HikariCP pooling:

```kotlin
val jdbiProvider = JdbiProvider(
    config = DatabaseConfig(
        url = "jdbc:postgresql://localhost:5432/mydb",
        user = "postgres",
        password = "secret",
        maximumPoolSize = 10
    ),
    openTelemetry = otelProvider.get(),  // Optional: query tracing
    meterRegistry = meterRegistry         // Optional: pool metrics
)

val jdbi = jdbiProvider.get()
```

Features:
- HikariCP connection pooling
- Kotlin plugin pre-installed
- OpenTelemetry query tracing
- Micrometer metrics integration

### Virtual Thread Extensions

Execute JDBI operations on virtual threads to avoid blocking Netty's event loop:

```kotlin
// Suspend functions - run on virtual threads
suspend fun findUser(id: String): User? = jdbi.handle { handle ->
    handle.createQuery("SELECT * FROM users WHERE id = :id")
        .bind("id", id)
        .mapTo<User>()
        .findOne()
        .orElse(null)
}

suspend fun insertUser(user: User) = jdbi.use { handle ->
    handle.createUpdate("INSERT INTO users (id, name) VALUES (:id, :name)")
        .bindBean(user)
        .execute()
}
```

For tests or startup code where blocking is acceptable:

```kotlin
// Synchronous versions - block the calling thread
fun setupSchema() = jdbi.useSync { handle ->
    handle.execute("CREATE TABLE IF NOT EXISTS users (...)")
}

val count = jdbi.handleSync { handle ->
    handle.createQuery("SELECT count(*) FROM users")
        .mapTo<Long>()
        .one()
}
```

### Loom Dispatcher

The `Loom` dispatcher is also available for other blocking operations:

```kotlin
suspend fun <T> onVirtualThread(block: () -> T): T = withContext(Loom) { block() }
```

## Configuration

The `DatabaseConfig` is serializable for use with Ktor's configuration:

```hocon
database {
    url = "jdbc:postgresql://localhost:5432/mydb"
    user = "postgres"
    password = ${?DB_PASSWORD}
    maximumPoolSize = 5
}
```

```kotlin
val dbConfig: DatabaseConfig = config("database")
```

## Examples

See [JdbiProviderTest.kt](src/test/kotlin/krat/jdbi/JdbiProviderTest.kt) and [JdbiExtensionsTest.kt](src/test/kotlin/krat/jdbi/JdbiExtensionsTest.kt).

## License

MIT
