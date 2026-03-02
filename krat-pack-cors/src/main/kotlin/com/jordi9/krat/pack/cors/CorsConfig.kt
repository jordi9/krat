package com.jordi9.krat.pack.cors

import kotlinx.serialization.Serializable

@Serializable
data class CorsConfig(
  val allowedHosts: List<String> = emptyList(),
  val allowAnyLocalhost: Boolean = false
)
