package com.jordi9.krat.time

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.date.shouldBeAfter
import kotlinx.coroutines.delay

class SystemTimeTest : StringSpec({

  "now() returns the actual time" {
    val clock = SystemTime
    val firstInstant = clock.now()
    delay(2)

    clock.now() shouldBeAfter firstInstant
  }
})
