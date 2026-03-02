package com.jordi9.krat.time

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.date.shouldBeBefore
import io.kotest.matchers.shouldBe
import java.time.Duration
import java.time.Instant

class AdvancingTimeTest : StringSpec({

  "starts at initial instant" {
    val startInstant = Instant.parse("2021-01-01T00:00:00.00Z")
    val clock = AdvancingTime(startInstant)

    clock.now() shouldBe startInstant
  }

  "advance moves time forward" {
    val startInstant = Instant.parse("2021-01-01T00:00:00.00Z")
    val clock = AdvancingTime(startInstant)

    val firstInstant = clock.now()

    clock.advance(Duration.ofMillis(5000))
    val secondInstant = clock.now()

    clock.advance(Duration.ofMinutes(5))
    val thirdInstant = clock.now()

    firstInstant shouldBeBefore secondInstant
    firstInstant shouldBe secondInstant.minusMillis(5000)
    firstInstant shouldBe thirdInstant.minusMillis(5000).minus(Duration.ofMinutes(5))
  }
})
