package com.jordi9.krat.jdbi

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.util.concurrent.Executors

/**
 * Loom-based dispatcher using virtual threads.
 * Each task gets its own virtual thread - lightweight and ideal for blocking I/O.
 */
val Loom: CoroutineDispatcher =
  Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

/**
 * Execute a JDBI operation on a virtual thread.
 * Keeps Netty event loop free while JDBC blocking calls execute.
 */
suspend fun <T> Jdbi.handle(callback: Handle.() -> T): T = onVirtualThread { handleSync(callback) }

/**
 * Execute a JDBI operation on a virtual thread (no return value).
 */
suspend fun Jdbi.use(callback: Handle.() -> Unit) = onVirtualThread { useSync(callback) }

/**
 * Execute a JDBI operation synchronously (blocking the calling thread).
 * Use in tests or startup code where blocking is acceptable.
 */
fun <T> Jdbi.handleSync(callback: Handle.() -> T): T = withHandle<T, Exception>(callback)

/**
 * Execute a JDBI operation synchronously (no return value).
 * Use in tests or startup code where blocking is acceptable.
 */
fun Jdbi.useSync(callback: Handle.() -> Unit) = useHandle<Exception>(callback)

/**
 * Execute a blocking operation on a virtual thread.
 * Use for any code that blocks the calling thread (JDBC, file I/O, Process.waitFor()).
 */
private suspend fun <T> onVirtualThread(block: () -> T): T = withContext(Loom) { block() }
