package com.jordi9.kogiven

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class RequiredDelegateShould : StringSpec({

  "return the value once set" {
    var name: String by required()
    name = "j9"
    name shouldBe "j9"
  }

  "fail with property name when read before being set" {
    val error = shouldThrow<IllegalStateException> { Holder().foo }
    error.message shouldContain "foo"
  }

  "support other types" {
    var count: Int by required()
    count = 42
    count shouldBe 42
  }

  "fail with property name when set more than once" {
    val holder = Holder()
    holder.foo = "bar"
    val error = shouldThrow<IllegalStateException> { holder.foo = "second" }
    error.message shouldBe "foo has already been set"
  }
})

class Holder {
  var foo: String by required()
}
