# kogiven

BDD-style acceptance testing for [Kotest](https://kotest.io/).

Kogiven provides the Given/When/Then structure for writing component tests that exercise your application from the outside — through its public HTTP API, against a real (in-memory) database, with external dependencies stubbed. Tests read like specifications and run the same code path as production.

Inspired by [JGiven](https://jgiven.org/) and the acceptance testing patterns from [Growing Object-Oriented Software, Guided by Tests](http://www.growing-object-oriented-software.com/) by Nat Pryce and Steve Freeman.

## Installation

```kotlin
dependencies {
    testImplementation("com.jordi9:krat-kogiven:$version")
}
```

## Writing Acceptance Tests

### 1. Define a Context

The context carries state between stages within a single test:

```kotlin
class OrderContext {
    lateinit var status: HttpStatusCode
    lateinit var response: JsonResponse
    var insertedOrderId: OrderId by required()
}
```

### 2. Define Stages

Each stage is a class with methods that read like natural language:

```kotlin
class GivenOrder : StageContext<GivenOrder, OrderContext>() {
    fun `an order exists`(name: String) = apply {
        val row = Orders.inserted(name = name)
        ctx.insertedOrderId = row.id
    }
}

class WhenOrder : StageContext<WhenOrder, OrderContext>() {
    suspend fun `listing all orders`() = apply {
        val response = httpClient().get("/api/v1/orders")
        ctx.status = response.status
        ctx.response = response.toJsonResponse()
    }
}

class ThenOrder : StageContext<ThenOrder, OrderContext>() {
    fun `the response is successful`() = apply {
        ctx.status shouldBe HttpStatusCode.OK
    }

    fun `orders are returned`(count: Int) = apply {
        ctx.response.items().size shouldBe count
    }
}
```

### 3. Write Scenarios

Scenarios stay clean — all implementation details live in stages:

```kotlin
class OrderShould : ScenarioStringSpec<GivenOrder, WhenOrder, ThenOrder, OrderContext>({

    "return orders after creating them" {
        Given.`an order exists`("Widget")
        When.`listing all orders`()
        Then.`the response is successful`()
            .and().`orders are returned`(1)
    }
})
```

### File Organization

```
app/src/test/kotlin/scenario/
├── OrderStages.kt    # Context + Given/When/Then stages
└── OrderShould.kt    # Test scenarios only
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

Each test gets a fresh context instance — stages are automatically reset between tests.

### Type-Safe Stages

Generic type parameters ensure your stages work with the correct context type.

### FunSpec Support

```kotlin
class OrderShould : ScenarioFunSpec<GivenOrder, WhenOrder, ThenOrder, OrderContext>({
    test("return orders after creating them") {
        Given.`an order exists`("Widget")
        When.`listing all orders`()
        Then.`the response is successful`()
    }
})
```

## The Acceptance Testing Approach

Kogiven is designed for component tests that sit at the center of the [testing honeycomb](https://engineering.atspotify.com/2018/01/testing-of-microservices/):

```
       ┌─────────────────┐
       │   E2E (few)     │
       ├─────────────────┤
  ████████████████████████  ← Component tests (primary)
       ├─────────────────┤
       │  Unit (sparse)  │
       └─────────────────┘
```

The key idea: tests exercise the full application stack (HTTP handlers, use cases, repositories, database) with only external dependencies stubbed. This gives high confidence with fast, reliable tests.

### What Makes a Good Acceptance Test

- **Same code as production** — no test-only modules or conditional logic
- **Real database** — in-memory SQLite/H2, not mocked
- **External ports stubbed** — HTTP clients, notification services, file systems
- **Test through the public API** — HTTP requests in, HTTP responses out
- **Scenarios read like specs** — anyone can understand what's being tested

## Companion Libraries

- **[krat-pack-testlib](../krat-pack-testlib)** — HTTP response assertion helpers (`JsonResponse`, `toJsonResponse()`)
- **[krat-otel-testlib](../krat-otel-testlib)** — In-memory OpenTelemetry span capture
- **[krat-time-testlib](../krat-time-testlib)** — Fixed time for deterministic tests

## Examples

See [KogivenStringSpecShould.kt](src/test/kotlin/com/jordi9/kogiven/KogivenStringSpecShould.kt) and [KogivenSpecificStages.kt](src/test/kotlin/com/jordi9/kogiven/KogivenSpecificStages.kt).
