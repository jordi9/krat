package com.jordi9.krat.otel.canonicaltraces

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import com.jordi9.krat.logging.LogEventsExtension
import com.jordi9.krat.otel.LogFormat
import com.jordi9.krat.otel.OpenTelemetryConfig
import com.jordi9.krat.otel.OpenTelemetryProvider
import com.jordi9.krat.otel.appAttribute
import com.jordi9.krat.otel.traced
import com.jordi9.krat.otel.withCurrentSpan
import com.jordi9.krat.otel.withWorkerSpan
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
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
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Logger as LogbackLogger

class LoggingSpanProcessorTest : StringSpec({

  val logs = extension(LogEventsExtension("TraceLog"))

  "PRETTY format logs multi-line trace summary with status symbol" {
    prettyApp { get("/hello") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/hello")

      with(logs.events.single()) {
        level shouldBe Level.INFO
        message shouldContain "✓ [GET"
        message shouldContain "/hello"
        message shouldContain "200"
      }
    }
  }

  "LOGFMT format logs single-line key=value summary" {
    logfmtApp { get("/hello") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/hello")

      with(logs.events.single()) {
        level shouldBe Level.INFO
        message shouldContain "method=GET"
        message shouldContain "route=/hello"
        message shouldContain "status=200"
        message shouldContain "duration="
        message shouldContain "trace_id="
        message shouldNotContain "\n"
      }
    }
  }

  "logs at WARN level for 404 responses" {
    logfmtApp { get("/not-found") { call.respond(HttpStatusCode.NotFound, "response") } }.run {
      client.get("/not-found")

      with(logs.events.single()) {
        level shouldBe Level.WARN
        message shouldContain "404"
      }
    }
  }

  "logs at ERROR level for 500 responses" {
    logfmtApp { get("/error") { call.respond(HttpStatusCode.InternalServerError, "response") } }.run {
      client.get("/error")

      with(logs.events.single()) {
        level shouldBe Level.ERROR
        message shouldContain "500"
      }
    }
  }

  "PRETTY format includes trace ID" {
    prettyApp { get("/test") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/test")

      logs.events.single().message shouldContain Regex("[a-f0-9]{32}")
    }
  }

  "LOGFMT format includes trace ID" {
    logfmtApp { get("/test") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/test")

      logs.events.single().message shouldContain Regex("trace_id=[a-f0-9]{32}")
    }
  }

  "PRETTY format filters out noisy infrastructure attributes" {
    prettyApp { get("/api/users") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/api/users")

      with(logs.events.single().message) {
        shouldNotContain("url.scheme")
        shouldNotContain("server.address")
        shouldNotContain("network.protocol")
        shouldNotContain("user_agent")
      }
    }
  }

  "LOGFMT format includes only app attributes" {
    logfmtApp { get("/api/users") { call.respond(HttpStatusCode.OK, "response") } }.run {
      client.get("/api/users")

      with(logs.events.single().message) {
        shouldNotContain("url.scheme")
        shouldNotContain("server.address")
        shouldNotContain("http.request.method=")
      }
    }
  }

  "PRETTY format shows app attributes and child spans" {
    prettyApp { tracer ->
      get("/videos") {
        withCurrentSpan {
          appAttribute["userId"] = "user-123"
          appAttribute["count"] = 5
        }
        tracer.childSpan("database.query") {
          setAttribute("db.statement", "SELECT * FROM users")
          setAttribute("db.rows", 42)
        }
        tracer.childSpan("cache.lookup") {}
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/videos")

      with(logs.events.single().message) {
        shouldContain("app.userId=user-123")
        shouldContain("app.count=5")
        shouldContain("↳ database.query")
        shouldContain("""db.statement="SELECT * FROM users""")
        shouldContain("db.rows=42")
        shouldContain("↳ cache.lookup")
      }
    }
  }

  "LOGFMT format shows app attributes on root span using traced" {
    logfmtApp {
      get("/with-attrs") {
        Pair("bar", "qux").traced { result ->
          appAttribute["baz"] = result.first
          appAttribute["quux"] = result.second
        }
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/with-attrs")

      with(logs.events.single().message) {
        shouldContain("app.baz=bar")
        shouldContain("app.quux=qux")
      }
    }
  }

  "LOGFMT format includes child spans as key=value" {
    logfmtApp { tracer ->
      get("/children") {
        tracer.childSpan("database.query") {}
        tracer.childSpan("cache.lookup") {}
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/children")

      with(logs.events.single().message) {
        shouldContain("span.database.query=")
        shouldContain("span.cache.lookup=")
        shouldContain("ms")
        shouldNotContain("↳")
        shouldNotContain("\n")
      }
    }
  }

  "PRETTY format shows error symbol and stacktrace for errors" {
    prettyApp {
      get("/throws") {
        try {
          throw IllegalArgumentException("Something went wrong")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.InternalServerError, "error")
        }
      }
    }.run {
      client.get("/throws")

      with(logs.events.single().message) {
        shouldContain("✗ [GET")
        shouldContain("java.lang.IllegalArgumentException: Something went wrong")
        shouldContain("at ") // stacktrace frames
      }
    }
  }

  "PRETTY format shows warning symbol for 4xx responses" {
    prettyApp { get("/not-found") { call.respond(HttpStatusCode.NotFound, "response") } }.run {
      client.get("/not-found")

      logs.events.single().message shouldContain "⚠ [GET"
    }
  }

  "PRETTY format shows exception message but excludes stacktrace for 4xx" {
    prettyApp {
      get("/bad-request") {
        try {
          throw IllegalArgumentException("Invalid input")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.BadRequest, "error")
        }
      }
    }.run {
      client.get("/bad-request")

      with(logs.events.single().message) {
        shouldContain("⚠ [GET")
        shouldContain("400")
        shouldContain("java.lang.IllegalArgumentException: Invalid input")
        shouldNotContain("at ") // no stacktrace frames
      }
    }
  }

  "LOGFMT format includes exception type, message and stacktrace" {
    logfmtApp {
      get("/throws") {
        try {
          throw RuntimeException("Database connection failed")
        } catch (e: Exception) {
          Span.current().recordException(e)
          call.respond(HttpStatusCode.InternalServerError, "error")
        }
      }
    }.run {
      client.get("/throws")

      with(logs.events.single().message) {
        shouldContain("exception.type=java.lang.RuntimeException")
        shouldContain("""exception.message="Database connection failed"""")
        shouldContain("exception.stacktrace=")
        shouldContain("\\n") // newlines should be escaped
        shouldNotContain("\n") // no actual newlines in logfmt output
      }
    }
  }

  "LOGFMT format outputs valid logfmt with space separators" {
    logfmtApp {
      get("/logfmt") {
        withCurrentSpan {
          appAttribute["simple"] = "value"
          appAttribute["another"] = "test"
        }
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/logfmt")

      val event = logs.events.single()

      // Verify span log message content
      with(event.message) {
        shouldNotContain(", ") // logfmt uses space, not comma-space
        shouldContain(" app.simple=value")
        shouldContain(" app.another=test")
      }

      // Verify logback appender formats output as logfmt
      val context = LoggerFactory.getILoggerFactory() as LoggerContext

      @Suppress("UNCHECKED_CAST")
      val encoder = (
        context.getLogger(LogbackLogger.ROOT_LOGGER_NAME)
          .getAppender("CONSOLE") as ConsoleAppender<ILoggingEvent>
        ).encoder
      val formattedLine = String(encoder.encode(event))

      formattedLine shouldContain "ts="
      formattedLine shouldContain "level=INFO"
      formattedLine shouldContain "logger=TraceLog"
      formattedLine shouldContain "msg=method=GET"
    }
  }

  "LOGFMT format quotes values containing spaces" {
    logfmtApp {
      get("/spaces") {
        withCurrentSpan {
          appAttribute["query"] = "hello world"
          appAttribute["path"] = "some/path with spaces"
        }
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/spaces")

      with(logs.events.single().message) {
        shouldContain("""app.query="hello world"""")
        shouldContain("""app.path="some/path with spaces"""")
      }
    }
  }

  "PRETTY format aligns multi-line attribute values in child spans" {
    prettyApp { tracer ->
      get("/multiline") {
        tracer.childSpan("jdbi.Query") {
          setAttribute("sql", "SELECT *\nFROM users\nWHERE id = 1")
        }
        call.respond(HttpStatusCode.OK, "response")
      }
    }.run {
      client.get("/multiline")

      with(logs.events.single().message) {
        shouldContain("↳ jdbi.Query")
        shouldContain("""sql="SELECT *""")
        shouldContain("\n      FROM users")
        shouldContain("\n      WHERE id = 1")
      }
    }
  }

  "PRETTY format propagates worker context across coroutine thread switches" {
    workerApp(LogFormat.PRETTY) { tracer ->
      tracer.withWorkerSpan("worker.process_video") {
        setAttribute("app.videoId", "video-123")

        // Force thread switch - child spans should still link to parent
        withContext(Dispatchers.Default) {
          tracer.childSpan("ytdlp.download") {
            setAttribute("url", "https://youtube.com/watch?v=abc")
          }
        }

        withContext(Dispatchers.IO) {
          tracer.childSpan("ffmpeg.convert") {
            setAttribute("output", "file.flac")
          }
        }
      }
    }

    with(logs.events.single().message) {
      shouldContain("✓ [worker.process_video]")
      shouldContain("app.videoId=video-123")
      shouldContain("↳ ytdlp.download")
      shouldContain("""url="https://youtube.com/watch?v=abc"""")
      shouldContain("↳ ffmpeg.convert")
      shouldContain("output=file.flac")
    }
  }

  "PRETTY format records worker exceptions with stacktrace" {
    workerApp(LogFormat.PRETTY) { tracer ->
      runCatching {
        tracer.withWorkerSpan("worker.failing") {
          withContext(Dispatchers.Default) {
            throw IllegalStateException("Download failed")
          }
        }
      }
    }

    with(logs.events.single()) {
      level shouldBe Level.ERROR
      message shouldContain "✗ [worker.failing]"
      message shouldContain "ERROR"
      message shouldContain "java.lang.IllegalStateException: Download failed"
      message shouldContain "at " // stacktrace frames
    }
  }

  "LOGFMT format propagates context across coroutine thread switches" {
    workerApp(LogFormat.LOGFMT) { tracer ->
      tracer.withWorkerSpan("worker.process_video") {
        setAttribute("app.videoId", "video-123")

        withContext(Dispatchers.Default) {
          tracer.childSpan("ytdlp.download") {}
        }

        withContext(Dispatchers.IO) {
          tracer.childSpan("ffmpeg.convert") {}
        }
      }
    }

    with(logs.events.single().message) {
      shouldContain("worker=worker.process_video")
      shouldContain("outcome=OK")
      shouldContain("app.videoId=video-123")
      shouldContain("span.ytdlp.download=")
      shouldContain("span.ffmpeg.convert=")
      shouldContain("ms")
      shouldNotContain("\n")
    }
  }

  "LOGFMT format records worker exceptions" {
    workerApp(LogFormat.LOGFMT) { tracer ->
      runCatching {
        tracer.withWorkerSpan("worker.failing") {
          withContext(Dispatchers.Default) {
            throw IllegalStateException("Download failed")
          }
        }
      }
    }

    with(logs.events.single()) {
      level shouldBe Level.ERROR
      message shouldContain "worker=worker.failing"
      message shouldContain "outcome=ERROR"
      message shouldContain "exception.type=java.lang.IllegalStateException"
      message shouldContain """exception.message="Download failed""""
    }
  }

  "excludes health check routes from logging" {
    logfmtApp {
      get("/health") { call.respond(HttpStatusCode.OK, "ok") }
      get("/api/users") { call.respond(HttpStatusCode.OK, "users") }
    }.run {
      client.get("/health")
      client.get("/api/users")

      logs.events shouldHaveSize 1
      logs.events.single().message shouldContain "/api/users"
    }
  }
})

private suspend fun prettyApp(route: Route.(Tracer) -> Unit) = tracedApp(LogFormat.PRETTY, route)

private suspend fun logfmtApp(route: Route.(Tracer) -> Unit) = tracedApp(LogFormat.LOGFMT, route)

private fun tracedApp(format: LogFormat, route: Route.(Tracer) -> Unit): TracedApp {
  val provider = OpenTelemetryProvider(
    config = OpenTelemetryConfig(serviceName = "test-service", logFormat = format),
    spanProcessor = LoggingSpanProcessor(format = format)
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
    spanProcessor = LoggingSpanProcessor(format = format)
  ).use { provider ->
    val tracer = provider.get().getTracer("test")
    provider.use { _ ->
      runBlocking { block(tracer) }
    }
  }
}
