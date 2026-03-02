package com.jordi9.krat.pack.core

import io.ktor.server.application.ApplicationCall
import io.ktor.server.routing.Route
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import io.ktor.server.sse.sse as ktorSse

/**
 * Handler for Server-Sent Events (SSE) endpoints.
 * Similar to Handler but for streaming SSE events instead of request/response.
 */
interface SseHandler {
  /**
   * Produces a flow of SSE events to stream to the client.
   * The flow completes when the connection should close.
   */
  suspend fun events(call: ApplicationCall): Flow<ServerSentEvent>
}

fun Route.sse(path: String, handler: SseHandler) {
  ktorSse(path) {
    handler.events(call).collect { event -> send(event) }
  }
}
