package com.jordi9.krat.flyway

import kotlinx.serialization.Serializable
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationInfo
import org.flywaydb.core.api.output.MigrateResult

class DatabaseMigrations(
  jdbcUrl: String,
  user: String? = null,
  password: String? = null,
  private val config: DatabaseMigrationsConfig = DatabaseMigrationsConfig()
) {

  private val flyway = Flyway.configure()
    .dataSource(jdbcUrl, user, password)
    .locations(*config.locations.toTypedArray())
    .load()

  fun maybeMigrate(): MigrateResult? = if (config.autoApply) migrate() else null

  fun migrate(): MigrateResult = flyway.migrate()

  fun pending(): List<MigrationInfo> = flyway.info().pending().toList()
}

@Serializable
data class DatabaseMigrationsConfig(
  val autoApply: Boolean = true,
  val locations: List<String> = listOf("classpath:db/migration")
)
