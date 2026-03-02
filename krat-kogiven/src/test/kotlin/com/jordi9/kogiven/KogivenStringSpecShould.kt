package com.jordi9.kogiven

class KogivenStringSpecShould : ScenarioStringSpec<Given, When, Then, TestContext>({

  "write idiomatic testing while use context to pass results" {
    Given.`the app is running`()
    When.`browsing to root path`()
    Then.`request is accepted`()
  }

  "context must be unique between tests and we can concatenate steps" {
    Given.`an empty context`()
      .and().`a user`()
    When.`browsing to root path for user`()
    Then.`user already exists`()
  }

  "can capture exceptions" {
    Given.`the app is running`()
    When.`browsing to root path`()
    Then.`request is accepted`()
      .but().`error happened`()
  }

  "context can use any type" {
    Given.`a list of bars`()
    When.`browsing to root path`()
    Then.`bars exists`()
  }
})
