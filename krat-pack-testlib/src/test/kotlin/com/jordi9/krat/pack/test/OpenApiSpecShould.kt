package com.jordi9.krat.pack.test

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication

class OpenApiSpecShould : StringSpec({

  "collect method and path for each registered route" {
    withApplication({
      get("/items") { call.respondText("ok") }
      post("/items") { call.respondText("ok") }
      get("/items/{id}") { call.respondText("ok") }
    }) { app ->
      OpenApiSpec(app).registeredRoutes shouldBe setOf(
        RouteId(HttpMethod.Get, "/items"),
        RouteId(HttpMethod.Post, "/items"),
        RouteId(HttpMethod.Get, "/items/{id}")
      )
    }
  }

  "exclude routes under the default /docs and /static prefixes" {
    withApplication({
      get("/items") { call.respondText("ok") }
      get("/docs/swagger") { call.respondText("ok") }
      get("/static/openapi.yaml") { call.respondText("ok") }
    }) { app ->
      OpenApiSpec(app).registeredFilteredRoutes shouldBe setOf(
        RouteId(HttpMethod.Get, "/items")
      )
    }
  }

  "exclude routes under custom ignored prefixes" {
    withApplication({
      get("/items") { call.respondText("ok") }
      get("/internal/health") { call.respondText("ok") }
    }) { app ->
      OpenApiSpec(app, ignoredPrefixes = setOf("/internal")).registeredFilteredRoutes shouldBe setOf(
        RouteId(HttpMethod.Get, "/items")
      )
    }
  }

  "load documented routes from /static/openapi.yaml on the classpath" {
    withApplication({}) { app ->
      OpenApiSpec(app).documentedRoutes shouldBe setOf(
        RouteId(HttpMethod.Get, "/items"),
        RouteId(HttpMethod.Post, "/items"),
        RouteId(HttpMethod.Get, "/items/{id}"),
        RouteId(HttpMethod.Delete, "/items/{id}")
      )
    }
  }

  "match registered filtered routes against documented routes when the app implements the spec" {
    withApplication({
      get("/items") { call.respondText("ok") }
      post("/items") { call.respondText("ok") }
      get("/items/{id}") { call.respondText("ok") }
      delete("/items/{id}") { call.respondText("ok") }
      get("/docs/swagger") { call.respondText("ok") }
    }) { app ->
      val spec = OpenApiSpec(app)
      spec.registeredFilteredRoutes shouldBe spec.documentedRoutes
    }
  }
})

private fun withApplication(setup: Routing.() -> Unit, assertions: (Application) -> Unit) {
  testApplication {
    lateinit var app: Application
    application {
      app = this
      routing(setup)
    }
    startApplication()
    assertions(app)
  }
}
