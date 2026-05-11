package com.jordi9.krat.otel

import com.jordi9.krat.logging.LogEventsExtension
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse
import io.opentelemetry.proto.collector.trace.v1.TraceServiceGrpc
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.ServiceAttributes
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class OpenTelemetryProviderTest : StringSpec({

  val logs = extension(LogEventsExtension(CANONICAL_LOGGER))

  "creates valid OpenTelemetry instance" {
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(serviceName = "test-service"),
      spanProcessor = noopProcessor()
    )

    val otel = provider.get()

    otel shouldNotBe null
    provider.close()
  }

  "configures resource attributes correctly" {
    val exporter = InMemorySpanExporter.create()
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(
        serviceName = "my-service",
        serviceVersion = "2025.01.15",
        logFormat = LogFormat.NONE
      ),
      spanProcessor = SimpleSpanProcessor.create(exporter)
    )

    val tracer = provider.get().getTracer("test")
    tracer.spanBuilder("test-span").startSpan().end()

    val span = exporter.finishedSpanItems.single()
    span.resource.getAttribute(ServiceAttributes.SERVICE_NAME) shouldBe "my-service"
    span.resource.getAttribute(ServiceAttributes.SERVICE_VERSION) shouldBe "2025.01.15"

    provider.close()
  }

  "with LogFormat.NONE produces no logs" {
    val provider = OpenTelemetryProvider(
      config = OpenTelemetryConfig(serviceName = "silent-service", logFormat = LogFormat.NONE)
    )

    val tracer = provider.get().getTracer("test")
    tracer.spanBuilder("worker.test")
      .setSpanKind(SpanKind.CONSUMER)
      .startSpan()
      .end()

    logs.events.size shouldBe 0
    provider.close()
  }

  "default span processor wires both OTLP exporter and canonical logger when otlpEnabled is true" {
    //given:
    val received = LinkedBlockingQueue<ExportTraceServiceRequest>()

    val server = NettyServerBuilder.forPort(0)
      .addService(TraceServiceStub(received))
      .build()
      .start()

    val otel = OpenTelemetryProvider(
      config = OpenTelemetryConfig(
        serviceName = "otlp-test",
        otlpEnabled = true,
        otlpEndpoint = "http://localhost:${server.port}",
        logFormat = LogFormat.LOGFMT
      )
    )

    //when:
    otel.get()
      .getTracer("test")
      .withWorkerSpan("worker.test") {
        //span created
      }
    otel.close()

    //then:
    val request = received.poll(5, TimeUnit.SECONDS)
    request.shouldNotBeNull()

    val span = request.resourceSpansList.single().scopeSpansList.single().spansList.single()
    span.name shouldBe "worker.test"
    logs.events.size shouldBe 1

    //cleanup:
    server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
  }

  "OpenTelemetryConfig has sensible defaults" {
    val config = OpenTelemetryConfig(serviceName = "my-app")

    config.otlpEndpoint shouldBe "http://localhost:4317"
    config.otlpEnabled shouldBe false
    config.logFormat shouldBe LogFormat.PRETTY
  }
})

private fun noopProcessor() = SimpleSpanProcessor.create(InMemorySpanExporter.create())

private class TraceServiceStub(private val received: LinkedBlockingQueue<ExportTraceServiceRequest>) :
  TraceServiceGrpc.TraceServiceImplBase() {

  override fun export(
    request: ExportTraceServiceRequest,
    responseObserver: StreamObserver<ExportTraceServiceResponse>
  ) {
    received.add(request)
    responseObserver.onNext(ExportTraceServiceResponse.getDefaultInstance())
    responseObserver.onCompleted()
  }
}
