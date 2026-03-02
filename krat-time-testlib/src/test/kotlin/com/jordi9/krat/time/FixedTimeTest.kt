package com.jordi9.krat.time

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import java.time.Instant

class FixedTimeTest : StringSpec({

  "returns the same fixed time" {
    val fixedInstant = Instant.parse("2021-01-01T00:00:00.00Z")
    val clock = FixedTime(fixedInstant)

    val firstInstant = clock.now()
    delay(2)
    val secondInstant = clock.now()

    firstInstant shouldBe fixedInstant
    secondInstant shouldBe fixedInstant
  }
})
