package com.jordi9.krat.pack.core

import io.ktor.server.application.Application
import io.ktor.server.config.getAs

inline fun <reified T> Application.config(path: String): T = environment.config.property(path).getAs<T>()
