package com.jordi9.kogiven

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

class Given : StageContext<Given, TestContext>() {

  fun `the app is running`() {
    ctx.baz = "baz"
  }

  fun `an empty context`() = apply {
    ctx.isNotInitialized() shouldBe true
    ctx.baz shouldBe "some default"
  }

  fun `a user`() {
    ctx.userId = "j9"
  }

  fun `a list of bars`() = apply {
    ctx.bar += listOf("bar", "bar")
  }
}

class When : StageContext<When, TestContext>() {

  fun `browsing to root path`() {
    ctx.status = Status.OK
  }

  fun `browsing to root path for user`() {
    ctx.status = Status.ALREADY_REPORTED
  }
}

class Then : StageContext<Then, TestContext>() {

  fun `request is accepted`() = apply {
    ctx.baz shouldBe "baz"
    ctx.status shouldBe Status.OK
  }

  fun `user already exists`() {
    //different assert style just for the sake of it
    ctx.apply {
      userId shouldBe "j9"
      status shouldBe Status.ALREADY_REPORTED
    }
  }

  fun `error happened`() {
    shouldThrow<UninitializedPropertyAccessException> {
      ctx.userId
    }
  }

  fun `bars exists`() {
    ctx.bar shouldBe listOf("bar", "bar")
  }
}

class TestContext {

  lateinit var userId: String
  lateinit var status: Status
  var bar: List<String> = emptyList()
  var baz: String = "some default"

  fun isNotInitialized(): Boolean = this::userId.isInitialized.not()
}

enum class Status {
  OK,
  ALREADY_REPORTED
}
