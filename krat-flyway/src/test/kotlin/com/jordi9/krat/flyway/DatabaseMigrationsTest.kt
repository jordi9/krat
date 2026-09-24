package com.jordi9.krat.flyway

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.sql.DriverManager

class DatabaseMigrationsTest : StringSpec({
  "disabled startup leaves migrations pending but explicit migrate still applies them" {
    val url = "jdbc:sqlite:${tempfile(suffix = ".db").absolutePath}"
    val migrations = DatabaseMigrations(url, config = DatabaseMigrationsConfig(autoApply = false))

    migrations.maybeMigrate() shouldBe null
    migrations.pending().map { it.version.version } shouldContainExactly listOf("1")
    tableExists(url, "notes") shouldBe false

    migrations.migrate().migrationsExecuted shouldBe 1
    tableExists(url, "notes") shouldBe true
    migrations.pending() shouldBe emptyList()
    migrations.migrate().migrationsExecuted shouldBe 0
  }

  "startup applies migrations from the configured location" {
    val url = "jdbc:sqlite:${tempfile(suffix = ".db").absolutePath}"
    val migrations = DatabaseMigrations(
      url,
      config = DatabaseMigrationsConfig(locations = listOf("classpath:db/alternate"))
    )

    migrations.maybeMigrate()?.migrationsExecuted shouldBe 1
    tableExists(url, "items") shouldBe true
    tableExists(url, "notes") shouldBe false
    migrations.pending() shouldBe emptyList()
  }
})

private fun tableExists(url: String, table: String): Boolean = DriverManager.getConnection(url).use { connection ->
  connection.createStatement().use { statement ->
    statement.executeQuery("SELECT count(*) FROM sqlite_master WHERE name = '$table'").use { result ->
      result.next()
      result.getInt(1) == 1
    }
  }
}
