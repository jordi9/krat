package com.jordi9.krat.jdbi

import kotlinx.serialization.Serializable

@Serializable
data class DatabaseConfig(
  val url: String,
  val user: String? = null,
  val password: String? = null,
  val maximumPoolSize: Int = 2
)
