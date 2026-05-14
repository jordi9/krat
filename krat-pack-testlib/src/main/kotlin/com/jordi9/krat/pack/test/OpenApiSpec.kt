package com.jordi9.krat.pack.test

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.yamlMap
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.routing.HttpMethodRouteSelector
import io.ktor.server.routing.OpenApiRoutePathFormat
import io.ktor.server.routing.getAllRoutes
import io.ktor.server.routing.path
import io.ktor.server.routing.routingRoot

class OpenApiSpec(
  application: Application,
  private val ignoredPrefixes: Set<String> = setOf("/docs", "/static")
) {

  val registeredRoutes: Set<RouteId> = application
    .routingRoot
    .getAllRoutes()
    .mapNotNull { node ->
      val selector = node.selector as? HttpMethodRouteSelector ?: return@mapNotNull null
      val path = node.parent?.path(OpenApiRoutePathFormat).orEmpty().ifEmpty { "/" }
      RouteId(selector.method, path)
    }
    .toSet()

  val registeredFilteredRoutes = registeredRoutes
    .filterNot { it.isIgnored() }.toSet()

  val documentedRoutes: Set<RouteId> = loadSpec()

  private fun loadSpec(): Set<RouteId> {
    val methodsByName: Map<String, HttpMethod> = HttpMethod.DefaultMethods.associateBy { it.value }

    val spec = object {}.javaClass.getResourceAsStream("/static/openapi.yaml")
      ?.use { it.bufferedReader().readText() }
      .let { text ->
        requireNotNull(text) { "static/openapi.yaml not found on classpath" }

        Yaml.default.parseToYamlNode(text)
      }

    return spec.yamlMap.get<YamlMap>("paths")
      .let { it?.entries ?: error("openapi.yaml has no 'paths' section") }
      .entries
      .flatMap { (pathKey, opsNode) ->
        opsNode.yamlMap.entries.keys
          .mapNotNull { methodsByName[it.content.uppercase()] }
          .map { method -> RouteId(method, pathKey.content) }
      }
      .toSet()
  }

  fun RouteId.isIgnored(): Boolean = ignoredPrefixes.any { path == it || path.startsWith("$it/") } ||
    "..." in path
}

data class RouteId(val method: HttpMethod, val path: String)
