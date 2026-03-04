package com.jordi9.krat.pack.test

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class JsonResponseShould : StringSpec({

  "parse string field" {
    val response = JsonResponse("""{"name": "Widget"}""")
    response.string("name") shouldBe "Widget"
  }

  "parse nullable string field" {
    val response = JsonResponse("""{"name": "Widget", "description": null}""")
    response.stringOrNull("name") shouldBe "Widget"
    response.stringOrNull("description") shouldBe null
    response.stringOrNull("missing") shouldBe null
  }

  "parse int field" {
    val response = JsonResponse("""{"count": 42}""")
    response.int("count") shouldBe 42
  }

  "parse long field" {
    val response = JsonResponse("""{"id": 123456789}""")
    response.long("id") shouldBe 123456789L
  }

  "parse nested object" {
    val response = JsonResponse("""{"item": {"name": "Widget", "count": 3}}""")
    val item = response.obj("item")
    item.string("name") shouldBe "Widget"
    item.int("count") shouldBe 3
  }

  "parse array of items" {
    val response = JsonResponse("""[{"name": "A"}, {"name": "B"}]""")
    response.items().size shouldBe 2
    response.items()[0].string("name") shouldBe "A"
    response.items()[1].string("name") shouldBe "B"
    response.size shouldBe 2
    response.isEmpty() shouldBe false
  }

  "parse empty array" {
    val response = JsonResponse("""[]""")
    response.isEmpty() shouldBe true
    response.size shouldBe 0
  }

  "parse item with nullable string" {
    val response = JsonResponse("""[{"name": "A", "note": null}]""")
    val item = response.items()[0]
    item.stringOrNull("note") shouldBe null
    item.stringOrNull("missing") shouldBe null
  }
})
