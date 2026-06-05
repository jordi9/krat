package com.jordi9.krat.pack.test

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.post
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class JsonRequestShould : StringSpec({

  "send trimmed json body with application json content type" {
    var receivedContentType: ContentType? = null
    var receivedBody: String? = null

    testApplication {
      application {
        routing {
          post("/items") {
            receivedContentType = call.request.contentType()
            receivedBody = call.receiveText()
            call.respondText("ok")
          }
        }
      }

      val response = client.post("/items") {
        jsonBody(
          """
            {
              "name": "Cash"
            }
          """
        )
      }

      response.status shouldBe HttpStatusCode.OK
      receivedContentType shouldBe ContentType.Application.Json
      receivedBody shouldBe
        """
          {
            "name": "Cash"
          }
        """.trimIndent()
    }
  }
})
