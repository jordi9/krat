package com.jordi9.krat.jdbi

import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempfile
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.kotlin.mapTo
import java.io.File

class JdbiExtensionsTest : StringSpec() {

  override fun isolationMode(): IsolationMode = IsolationMode.InstancePerRoot

  val dbFile: File = tempfile(prefix = "test-", suffix = ".db")
  val jdbi = JdbiProvider(DatabaseConfig(url = "jdbc:sqlite:${dbFile.absolutePath}")).get()

  init {
    "handle executes on virtual thread and returns result" {
      val result = jdbi.handle {
        val isVirtual = Thread.currentThread().isVirtual
        createQuery("SELECT 1 as value").mapTo<Int>().one() to isVirtual
      }

      result.first shouldBe 1
      result.second shouldBe true
    }

    "use executes on virtual thread" {
      var executedOnVirtualThread = false

      jdbi.use {
        executedOnVirtualThread = Thread.currentThread().isVirtual
        execute("SELECT 1")
      }

      executedOnVirtualThread shouldBe true
    }

    "handleSync executes synchronously and returns result" {
      val result = jdbi.handleSync {
        createQuery("SELECT 99 as value").mapTo<Int>().one()
      }

      result shouldBe 99
    }

    "useSync executes synchronously" {
      var executed = false

      jdbi.useSync {
        executed = true
        execute("SELECT 1")
      }

      executed shouldBe true
    }

    "Loom dispatcher uses virtual threads" {
      val isVirtual = withContext(Loom) {
        Thread.currentThread().isVirtual
      }

      isVirtual shouldBe true
    }
  }
}
