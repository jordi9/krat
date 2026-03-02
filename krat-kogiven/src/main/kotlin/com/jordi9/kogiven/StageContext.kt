package com.jordi9.kogiven

open class StageContext<T, C> {
  lateinit var ctx: C & Any

  @Suppress("UNCHECKED_CAST")
  fun and(): T = this as T

  @Suppress("UNCHECKED_CAST")
  fun but(): T = this as T
}
