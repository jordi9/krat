package com.jordi9.kogiven

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.test.TestCase
import java.lang.reflect.ParameterizedType

@Suppress("PropertyName")
abstract class ScenarioStringSpec<G, W, T, C : Any>(
  body: ScenarioStringSpec<G, W, T, C>.() -> Unit = {}
) : StringSpec({}) where G : StageContext<G, C>, W : StageContext<W, C>, T : StageContext<T, C> {

  private val typeParams: List<Class<*>> by lazy { extractTypeParameters(this::class.java) }

  @Suppress("UNCHECKED_CAST")
  val Given: G = create(typeParams[0] as Class<G>)

  @Suppress("UNCHECKED_CAST")
  val When: W = create(typeParams[1] as Class<W>)

  @Suppress("UNCHECKED_CAST")
  val Then: T = create(typeParams[2] as Class<T>)

  @Suppress("UNCHECKED_CAST")
  private val ctxType = typeParams[3] as Class<C>

  init {
    body()
  }

  override suspend fun beforeEach(testCase: TestCase) {
    initContext(Given, When, Then, ctxType)
  }
}

@Suppress("PropertyName")
abstract class ScenarioFunSpec<G, W, T, C : Any>(
  body: ScenarioFunSpec<G, W, T, C>.() -> Unit = {}
) : FunSpec({}) where G : StageContext<G, C>, W : StageContext<W, C>, T : StageContext<T, C> {

  private val typeParams: List<Class<*>> by lazy { extractTypeParameters(this::class.java) }

  @Suppress("UNCHECKED_CAST")
  val Given: G = create(typeParams[0] as Class<G>)

  @Suppress("UNCHECKED_CAST")
  val When: W = create(typeParams[1] as Class<W>)

  @Suppress("UNCHECKED_CAST")
  val Then: T = create(typeParams[2] as Class<T>)

  @Suppress("UNCHECKED_CAST")
  private val ctxType = typeParams[3] as Class<C>

  init {
    body()
  }

  override suspend fun beforeEach(testCase: TestCase) {
    initContext(Given, When, Then, ctxType)
  }
}

private fun extractTypeParameters(clazz: Class<*>): List<Class<*>> {
  val superclass = clazz.genericSuperclass as ParameterizedType
  return superclass.actualTypeArguments.map { it as Class<*> }
}

@Suppress("UNCHECKED_CAST")
private fun <E> create(clazz: Class<in E>): E = clazz.getDeclaredConstructor().newInstance() as E

private fun <G : StageContext<G, C>, W : StageContext<W, C>, T : StageContext<T, C>, C> initContext(
  given: G,
  whenz: W,
  then: T,
  ctxType: Class<in C & Any>
) {
  val ctx = create(ctxType)
  given.ctx = ctx
  whenz.ctx = ctx
  then.ctx = ctx
}
