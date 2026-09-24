# krat-flyway

Runs Flyway migrations using a JDBC URL. Call `migrateOnStartup()` at application startup to apply migrations when `autoApply` is enabled (the default). Set it to `false` to leave migrations pending at startup; an explicit `migrate()` always applies them.

```kotlin
import com.jordi9.krat.flyway.DatabaseMigrations
import com.jordi9.krat.flyway.DatabaseMigrationsConfig

val migrations = DatabaseMigrations(
  jdbcUrl = database.url,
  user = database.user,
  password = database.password,
  config = DatabaseMigrationsConfig(
    autoApply = true,
    locations = listOf("classpath:db/migration")
  )
)

migrations.migrateOnStartup() // Returns null when autoApply is false.
val pending = migrations.pending()
val result = migrations.migrate() // Runs even when autoApply is false.
```

`DatabaseMigrationsConfig` is serializable for Ktor's `config<T>()`, for example from the `databaseMigrations` HOCON key. Database connection settings remain separate. The default location is `classpath:db/migration`. Include your JDBC driver in the application dependencies.
