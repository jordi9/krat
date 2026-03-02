package com.jordi9.krat.time

import java.time.Duration
import java.time.Instant

class AdvancingTime(startInstant: Instant) : TimeClock {
  private var current = startInstant

  fun advance(duration: Duration) {
    current = current.plus(duration)
  }

  override fun now(): Instant = current
}
