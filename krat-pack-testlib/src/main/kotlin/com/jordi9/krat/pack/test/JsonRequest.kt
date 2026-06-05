package com.jordi9.krat.pack.test

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

fun HttpRequestBuilder.jsonBody(body: String) {
  contentType(ContentType.Application.Json)
  setBody(body.trimIndent())
}
