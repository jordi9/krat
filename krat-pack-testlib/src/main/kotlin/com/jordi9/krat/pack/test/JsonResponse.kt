package com.jordi9.krat.pack.test

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

class JsonResponse(body: String) {
  private val element = Json.parseToJsonElement(body)

  fun string(key: String): String =
    stringOrNull(key) ?: throw NoSuchElementException("Key $key is missing in the object.")

  fun stringOrNull(key: String): String? {
    val value = element.jsonObject[key] ?: return null
    if (value is JsonNull) return null
    return value.jsonPrimitive.content
  }

  fun int(key: String): Int = element.jsonObject.getValue(key).jsonPrimitive.int

  fun long(key: String): Long = element.jsonObject.getValue(key).jsonPrimitive.long

  fun obj(key: String): JsonItem = JsonItem(element.jsonObject.getValue(key).jsonObject)

  fun items(): List<JsonItem> = element.jsonArray.map { JsonItem(it.jsonObject) }

  val size: Int get() = element.jsonArray.size

  fun isEmpty(): Boolean = element.jsonArray.isEmpty()
}

class JsonItem(private val obj: JsonObject) {
  fun string(key: String): String = obj.getValue(key).jsonPrimitive.content

  fun stringOrNull(key: String): String? {
    val value = obj[key] ?: return null
    if (value is JsonNull) return null
    return value.jsonPrimitive.content
  }

  fun int(key: String): Int = obj.getValue(key).jsonPrimitive.int

  fun long(key: String): Long = obj.getValue(key).jsonPrimitive.long
}

suspend fun HttpResponse.toJsonResponse() = JsonResponse(this.bodyAsText())
