package com.jordi9.kogiven

class KogivenFunSpecShould : ScenarioFunSpec<Given, When, Then, TestContext>({

  test("write idiomatic testing while use context to pass results") {
    Given.`the app is running`()
    When.`browsing to root path`()
    Then.`request is accepted`()
  }

  test("context must be unique between tests and we can concatenate steps") {
    Given.`an empty context`()
      .and().`a user`()
    When.`browsing to root path for user`()
    Then.`user already exists`()
  }

  test("can capture exceptions") {
    Given.`the app is running`()
    When.`browsing to root path`()
    Then.`request is accepted`()
      .but().`error happened`()
  }

  test("context can use any type") {
    Given.`a list of bars`()
    When.`browsing to root path`()
    Then.`bars exists`()
  }
})
