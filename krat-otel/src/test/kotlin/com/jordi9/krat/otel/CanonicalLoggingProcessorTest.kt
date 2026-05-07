package com.jordi9.krat.otel

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import com.github.benmanes.caffeine.cache.Ticker
import com.jordi9.krat.logging.LogEventsExtension
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.testing.testApplication
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.time.Duration

class CanonicalLoggingProcessorTest : StringSpec({

  val logs = extension(LogEventsExtension(CANONICAL_LOGGER))

  "LOGFMT canonical line for HTTP server: happy path, 4xx, and 5xx with exception" {
    logfmtApp { tracer ->
      get("/users/{id}") {
        withCurrentSpan { appAttribute["userId"] = "u-123" }
        tracer.childSpan("db.query") {}
        call.respond(HttpStatusCode.OK, "ok")
      }
      get("/missing") { call.respond(HttpStatusCode.NotFound, "nf") }
      get("/boom") {
        try {
          throw RuntimeException("connection failed")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.InternalServerError, "err")
        }
      }
    }.run {
      //when:
      client.get("/users/42")
      client.get("/missing")
      client.get("/boom")

      //then:
      logs.events shouldHaveSize 3

      //and: 200 happy path: span attributes verbatim, app attrs, child spans, computed fields, filters
      with(logs.events[0]) {
        level shouldBe Level.INFO
        message shouldBe ""
        // OTel semantic-conv keys preserved verbatim
        mdcPropertyMap["http.request.method"] shouldBe "GET"
        mdcPropertyMap["http.route"] shouldBe "/users/{id}"
        mdcPropertyMap["http.response.status_code"] shouldBe "200"
        mdcPropertyMap shouldContainKey "url.path"
        mdcPropertyMap shouldContainKey "url.scheme"
        // Computed/added
        mdcPropertyMap shouldContainKey "name"
        mdcPropertyMap shouldContainKey "trace_id"
        mdcPropertyMap shouldContainKey "duration_ms"
        // App attributes + children
        mdcPropertyMap["app.userId"] shouldBe "u-123"
        mdcPropertyMap["span.0.name"] shouldBe "db.query"
        mdcPropertyMap shouldContainKey "span.0.duration_ms"
        // Filtered prefixes
        mdcPropertyMap shouldNotContainKey "network.protocol.version"
        mdcPropertyMap shouldNotContainKey "user_agent.original"
      }

      //and: 4xx → WARN
      with(logs.events[1]) {
        level shouldBe Level.WARN
        mdcPropertyMap["http.response.status_code"] shouldBe "404"
      }

      //and: 5xx with recorded exception → ERROR + exception fields
      with(logs.events[2]) {
        level shouldBe Level.ERROR
        mdcPropertyMap["http.response.status_code"] shouldBe "500"
        mdcPropertyMap["exception.type"] shouldBe "java.lang.RuntimeException"
        mdcPropertyMap["exception.message"] shouldBe "connection failed"
        mdcPropertyMap shouldContainKey "exception.stacktrace"
      }
    }
  }

  "LOGFMT rendered output is valid single-line space-separated logfmt" {
    logfmtApp {
      get("/test") {
        withCurrentSpan { appAttribute["query"] = "hello world" }
        call.respond(HttpStatusCode.OK, "ok")
      }
      get("/throws") {
        try {
          throw RuntimeException("You're killing independent George.")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.InternalServerError, "err")
        }
      }
    }.run {
      client.get("/test")
      client.get("/throws")

      val happy = logs.events[0].render()
      happy shouldContain "ts="
      happy shouldContain "level=INFO"
      happy shouldContain "logger=CanonicalTrace"
      happy shouldContain "http.request.method=GET"
      happy shouldContain """app.query="hello world"""" // values with spaces are quoted
      happy shouldNotContain "msg=http" // msg is empty
      happy shouldNotContain ", " // space separator only
      happy.trimEnd() shouldNotContain "\n"

      val errored = logs.events[1].render()
      errored shouldContain "level=ERROR"
      errored shouldContain "java.lang.RuntimeException: You're killing independent George."
      errored shouldContain "at com.jordi9.krat.otel" // stacktrace flattened onto one line
      errored shouldNotContain "\\n" // multi-line values are normalized, not escaped
      errored.trimEnd() shouldNotContain "\n"
    }
  }

  "LOGFMT children are indexed (no name collisions) and multi-line attr values are flattened" {
    logfmtApp { tracer ->
      get("/items") {
        tracer.childSpan("db.query") {
          setAttribute("db.statement", "SELECT *\n  FROM users\n  WHERE id = ?")
        }
        tracer.childSpan("db.query") {
          setAttribute("db.statement", "UPDATE posts SET title = ?")
        }
        call.respond(HttpStatusCode.OK, "ok")
      }
    }.run {
      client.get("/items")

      with(logs.events.single().mdcPropertyMap) {
        // both children survive despite sharing a name
        get("span.0.name") shouldBe "db.query"
        get("span.1.name") shouldBe "db.query"
        this shouldContainKey "span.0.duration_ms"
        this shouldContainKey "span.1.duration_ms"
        // child attrs are preserved AND multi-line values are normalized to a single line
        get("span.0.db.statement") shouldBe "SELECT * FROM users WHERE id = ?"
        get("span.1.db.statement") shouldBe "UPDATE posts SET title = ?"
      }
    }
  }

  "LOGFMT canonical line for worker (CONSUMER): happy path and failing" {
    workerApp(LogFormat.LOGFMT) { tracer ->
      tracer.withWorkerSpan("worker.process_video") {
        setAttribute("app.videoId", "video-123")
        withContext(Dispatchers.Default) {
          tracer.childSpan("ytdlp.download") {}
        }
      }
      runCatching {
        tracer.withWorkerSpan("worker.failing") {
          throw IllegalStateException("download failed")
        }
      }
    }

    logs.events shouldHaveSize 2

    with(logs.events[0]) {
      level shouldBe Level.INFO
      mdcPropertyMap["name"] shouldBe "worker.process_video"
      mdcPropertyMap["outcome"] shouldBe "OK"
      mdcPropertyMap["app.videoId"] shouldBe "video-123"
      mdcPropertyMap["span.0.name"] shouldBe "ytdlp.download"
      mdcPropertyMap shouldContainKey "span.0.duration_ms"
    }

    with(logs.events[1]) {
      level shouldBe Level.ERROR
      mdcPropertyMap["name"] shouldBe "worker.failing"
      mdcPropertyMap["outcome"] shouldBe "ERROR"
      mdcPropertyMap["exception.type"] shouldBe "java.lang.IllegalStateException"
      mdcPropertyMap["exception.message"] shouldBe "download failed"
    }
  }

  "PRETTY canonical line for HTTP server: happy path with multi-line attr, 4xx, 5xx" {
    prettyApp { tracer ->
      get("/users/{id}") {
        withCurrentSpan { appAttribute["userId"] = "u-123" }
        tracer.childSpan("db.query") {
          setAttribute("sql", "SELECT *\nFROM users\nWHERE id = ?")
        }
        call.respond(HttpStatusCode.OK, "ok")
      }
      get("/bad") {
        try {
          throw IllegalArgumentException("bad input")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.BadRequest, "err")
        }
      }
      get("/boom") {
        try {
          throw RuntimeException("crash")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.InternalServerError, "err")
        }
      }
    }.run {
      client.get("/users/42")
      client.get("/bad")
      client.get("/boom")

      logs.events shouldHaveSize 3

      // Happy: ✓ symbol, header line, app attrs, child span with multi-line value, no infra noise
      with(logs.events[0]) {
        level shouldBe Level.INFO
        message shouldContain "✓ [GET /users/{id}] (200)"
        message shouldContain Regex("[a-f0-9]{32}")
        message shouldContain "app.userId=u-123"
        message shouldContain "↳ db.query"
        message shouldContain """sql="SELECT *"""
        message shouldContain "\n      FROM users" // continuation indented
        // Infra prefixes filtered from body
        message shouldNotContain "url.scheme"
        message shouldNotContain "server.address"
        message shouldNotContain "user_agent"
      }

      // 4xx: ⚠ symbol, exception message, NO stacktrace
      with(logs.events[1]) {
        level shouldBe Level.WARN
        message shouldContain "⚠ [GET /bad] (400)"
        message shouldContain "java.lang.IllegalArgumentException: bad input"
        message shouldNotContain "at " // stacktrace not included for WARN
      }

      // 5xx: ✗ symbol, exception message, WITH stacktrace
      with(logs.events[2]) {
        level shouldBe Level.ERROR
        message shouldContain "✗ [GET /boom] (500)"
        message shouldContain "java.lang.RuntimeException: crash"
        message shouldContain "at " // stacktrace included for ERROR
      }
    }
  }

  "PRETTY canonical line for worker (CONSUMER): happy path and failing with stacktrace" {
    workerApp(LogFormat.PRETTY) { tracer ->
      tracer.withWorkerSpan("worker.process_video") {
        setAttribute("app.videoId", "video-123")
        withContext(Dispatchers.Default) {
          tracer.childSpan("ytdlp.download") {
            setAttribute("url", "https://example.com/?ref=abc")
          }
        }
      }
      runCatching {
        tracer.withWorkerSpan("worker.failing") {
          withContext(Dispatchers.Default) {
            throw IllegalStateException("download failed")
          }
        }
      }
    }

    logs.events shouldHaveSize 2

    with(logs.events[0].message) {
      shouldContain("✓ [worker.process_video]")
      shouldContain("OK")
      shouldContain("app.videoId=video-123")
      shouldContain("↳ ytdlp.download")
      shouldContain("""url="https://example.com/?ref=abc"""")
    }

    with(logs.events[1]) {
      level shouldBe Level.ERROR
      message shouldContain "✗ [worker.failing]"
      message shouldContain "ERROR"
      message shouldContain "java.lang.IllegalStateException: download failed"
      message shouldContain "at "
    }
  }

  "orphan trace logged on timeout and on forceFlush" {
    val ticker = TestTicker()
    val processor = CanonicalLoggingProcessor(
      format = LogFormat.LOGFMT,
      orphanTimeout = Duration.ofMinutes(5),
      ticker = ticker
    )
    OpenTelemetryProvider(
      config = OpenTelemetryConfig(serviceName = "test-service", logFormat = LogFormat.LOGFMT),
      spanProcessor = processor
    ).use { provider ->
      val tracer = provider.get().getTracer("test")

      // forceFlush drains in-flight orphans before timeout
      tracer.spanBuilder("inflight.work").setSpanKind(SpanKind.INTERNAL).startSpan().end()
      logs.events.shouldBeEmpty()
      processor.forceFlush()

      with(logs.events.single()) {
        level shouldBe Level.WARN
        mdcPropertyMap["event"] shouldBe "trace_orphaned"
        mdcPropertyMap["span.0.name"] shouldBe "inflight.work"
      }

      // expireAfterWrite triggers orphan via the eviction listener
      tracer.spanBuilder("orphan.work").setSpanKind(SpanKind.INTERNAL).startSpan().end()
      ticker.advance(Duration.ofMinutes(6))
      processor.forceFlush()

      with(logs.events[1]) {
        level shouldBe Level.WARN
        mdcPropertyMap["event"] shouldBe "trace_orphaned"
        mdcPropertyMap["span.0.name"] shouldBe "orphan.work"
      }
    }
  }
})

private suspend fun prettyApp(route: Route.(Tracer) -> Unit) = tracedApp(LogFormat.PRETTY, route)

private suspend fun logfmtApp(route: Route.(Tracer) -> Unit) = tracedApp(LogFormat.LOGFMT, route)

private fun ILoggingEvent.render(): String = String(buildLogfmtEncoder().encode(this))

private class TestTicker : Ticker {
  private var nanos = 0L
  override fun read() = nanos
  fun advance(duration: Duration) {
    nanos += duration.toNanos()
  }
}

private fun tracedApp(format: LogFormat, route: Route.(Tracer) -> Unit): TracedApp {
  val provider = OpenTelemetryProvider(
    config = OpenTelemetryConfig(serviceName = "test-service", logFormat = format),
    spanProcessor = CanonicalLoggingProcessor(format = format)
  )
  val tracer = provider.get().getTracer("test")

  return TracedApp { test ->
    provider.use { provider ->
      testApplication {
        install(KtorServerTelemetry) { setOpenTelemetry(provider.get()) }
        routing { route(tracer) }
        test(client)
      }
    }
  }
}

private fun interface TracedApp {
  suspend fun execute(test: suspend (HttpClient) -> Unit)
}

private suspend fun TracedApp.run(test: suspend TestScope.() -> Unit) {
  execute { client -> TestScope(client).test() }
}

private class TestScope(val client: HttpClient)

private inline fun Tracer.childSpan(name: String, block: Span.() -> Unit) {
  spanBuilder(name).setSpanKind(SpanKind.CLIENT).startSpan().apply(block).end()
}

private fun workerApp(format: LogFormat, block: suspend (Tracer) -> Unit) {
  OpenTelemetryProvider(
    config = OpenTelemetryConfig(serviceName = "test-service", logFormat = format),
    spanProcessor = CanonicalLoggingProcessor(format = format)
  ).use { provider ->
    val tracer = provider.get().getTracer("test")
    runBlocking { block(tracer) }
  }
}
