package com.jordi9.krat.pack.core

import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put

fun Route.get(path: String, handler: Handler): Route = get(path) { handler.handle(call) }

fun Route.post(path: String, handler: Handler): Route = post(path) { handler.handle(call) }

fun Route.put(path: String, handler: Handler): Route = put(path) { handler.handle(call) }

fun Route.patch(path: String, handler: Handler): Route = patch(path) { handler.handle(call) }

fun Route.delete(path: String, handler: Handler): Route = delete(path) { handler.handle(call) }
