package com.jordi9.krat.pack.cors

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class CorsTest : FunSpec({

  context("allowAnyLocalhost accepts local origins") {
    withTests(
      mapOf(
        "http localhost" to "http://localhost:${randomPort()}",
        "https localhost" to "https://localhost:${randomPort()}",
        "http 127.0.0.1" to "http://127.0.0.1:${randomPort()}",
        "https 127.0.0.1" to "https://127.0.0.1:${randomPort()}"
      )
    ) { origin ->
      testApplication {
        application {
          installCors(CorsConfig(allowAnyLocalhost = true))
          routing { get("/test") { call.respondText("OK") } }
        }

        val response = client.options("/test") {
          header(HttpHeaders.Origin, origin)
          header(HttpHeaders.AccessControlRequestMethod, "GET")
        }

        response.status shouldBe HttpStatusCode.OK
        response.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe origin
      }
    }
  }

  test("allowAnyLocalhost rejects localhost-like malicious origins") {
    testApplication {
      application {
        installCors(CorsConfig(allowAnyLocalhost = true))
        routing { get("/test") { call.respondText("OK") } }
      }

      val response = client.options("/test") {
        header(HttpHeaders.Origin, "http://localhost.evil.com:3000")
        header(HttpHeaders.AccessControlRequestMethod, "GET")
      }

      response.status shouldBe HttpStatusCode.Forbidden
    }
  }
})

private fun randomPort(): Int = (1024..65535).random()
