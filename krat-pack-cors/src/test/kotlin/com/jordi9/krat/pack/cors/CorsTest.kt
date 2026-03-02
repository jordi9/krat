package com.jordi9.krat.pack.cors

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class CorsTest : StringSpec({

  "allowAnyLocalhost accepts localhost with any port" {
    testApplication {
      application {
        installCors(CorsConfig(allowAnyLocalhost = true))
        routing { get("/test") { call.respondText("OK") } }
      }

      val response = client.options("/test") {
        header(HttpHeaders.Origin, "http://localhost:3000")
        header(HttpHeaders.AccessControlRequestMethod, "GET")
      }

      response.status shouldBe HttpStatusCode.OK
      response.headers[HttpHeaders.AccessControlAllowOrigin] shouldBe "http://localhost:3000"
    }
  }

  "allowAnyLocalhost rejects localhost-like malicious origins" {
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
