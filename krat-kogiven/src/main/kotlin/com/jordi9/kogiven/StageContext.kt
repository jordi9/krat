package com.jordi9.kogiven

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class StageContext<T, C> {
  lateinit var ctx: C & Any

  @Suppress("UNCHECKED_CAST")
  fun and(): T = this as T

  @Suppress("UNCHECKED_CAST")
  fun but(): T = this as T
}

/**
 * Property delegate that fails with a descriptive error when read before being set.
 *
 * Use instead of `lateinit var` in test contexts, it produces clearer error messages that include the property name.
 */
class RequiredDelegate<T : Any> : ReadWriteProperty<Any?, T> {
  private var value: T? = null

  override fun getValue(thisRef: Any?, property: KProperty<*>): T = value ?: error("${property.name} has not been set")

  override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    check(this.value == null) { "${property.name} has already been set" }
    this.value = value
  }
}

fun <T : Any> required() = RequiredDelegate<T>()
