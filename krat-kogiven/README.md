# kogiven

BDD-style Given/When/Then test support for [Kotest](https://kotest.io/).

## Installation

```kotlin
dependencies {
    testImplementation("krat:kogiven:$version")
}
```

## Usage

### Define Your Stages

Create stage classes that extend `StageContext`:

```kotlin
// Shared context between stages
class OrderContext {
    lateinit var order: Order
    lateinit var result: OrderResult
    var discount: Double = 0.0
}

class Given : StageContext<Given, OrderContext>() {
    fun `an order with items`() = apply {
        ctx.order = Order(items = listOf(Item("Widget", 100)))
    }

    fun `a premium customer discount`() = apply {
        ctx.discount = 0.10
    }
}

class When : StageContext<When, OrderContext>() {
    fun `the order is submitted`() = apply {
        ctx.result = orderService.submit(ctx.order, ctx.discount)
    }
}

class Then : StageContext<Then, OrderContext>() {
    fun `the order is confirmed`() = apply {
        ctx.result.status shouldBe OrderStatus.CONFIRMED
    }

    fun `discount is applied`() = apply {
        ctx.result.total shouldBe 90.0
    }
}
```

### Write Tests

Use `ScenarioStringSpec` or `ScenarioFunSpec`:

```kotlin
class OrderTest : ScenarioStringSpec<Given, When, Then, OrderContext>({

    "order with discount is processed correctly" {
        Given.`an order with items`()
            .and().`a premium customer discount`()
        When.`the order is submitted`()
        Then.`the order is confirmed`()
            .and().`discount is applied`()
    }

    "order without discount uses full price" {
        Given.`an order with items`()
        When.`the order is submitted`()
        Then.`the order is confirmed`()
    }
})
```

Or with FunSpec style:

```kotlin
class OrderTest : ScenarioFunSpec<Given, When, Then, OrderContext>({

    test("order with discount is processed correctly") {
        Given.`an order with items`()
            .and().`a premium customer discount`()

        When.`the order is submitted`()

        Then.`the order is confirmed`()
            .and().`discount is applied`()
    }
})
```

## Features

### Fluent Chaining

Use `and()` and `but()` for readable chains:

```kotlin
Given.`a user`()
    .and().`a shopping cart`()
    .but().`no payment method`()
```

### Fresh Context Per Test

Each test gets a fresh context instance - stages are automatically reset between tests.

### Type-Safe Stages

Generic type parameters ensure your stages work with the correct context type.

## Why kogiven?

- **Clean separation** - Given/When/Then stages are distinct classes
- **Reusable steps** - Stage methods can be composed across tests
- **Readable tests** - Backtick method names read like documentation
- **Shared state** - Context object flows through all stages
- **Kotest integration** - Works with Kotest's lifecycle and assertions

## Examples

See [KogivenStringSpecShould.kt](src/test/kotlin/kogiven/KogivenStringSpecShould.kt) and [KogivenSpecificStages.kt](src/test/kotlin/kogiven/KogivenSpecificStages.kt).

## License

MIT
