package com.jordi9.krat.time

import java.time.Instant

interface TimeClock {
  fun now(): Instant
}

object SystemTime : TimeClock {
  override fun now(): Instant = Instant.now()
}
