package com.jordi9.krat.pack.core

import io.ktor.server.application.ApplicationCall

/**
 * The Ktor equivalent of the Ratpack Handler interface.
 */
interface Handler {
  suspend fun handle(call: ApplicationCall)
}
