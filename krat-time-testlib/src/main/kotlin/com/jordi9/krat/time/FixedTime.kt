package com.jordi9.krat.time

import java.time.Instant

class FixedTime(private val instant: Instant) : TimeClock {
  override fun now(): Instant = instant
}
