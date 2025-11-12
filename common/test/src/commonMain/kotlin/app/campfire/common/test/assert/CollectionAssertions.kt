package app.campfire.common.test.assert

import assertk.Assert
import assertk.assertions.support.expected
import assertk.assertions.support.show

inline fun <reified I> Assert<Collection<*>>.firstInstanceOf() = transform { actual ->
  actual.filterIsInstance<I>().firstOrNull()
    ?: expected("to contain ${I::class.simpleName}")
}

inline fun <reified I> Assert<Collection<*>>.containsInstance() = given { actual ->
  if (actual.filterIsInstance<I>().isNotEmpty()) return
  expected("to contain ${I::class.simpleName}")
}

inline fun <reified I> Assert<Collection<*>>.doesNotContainInstance() = given { actual ->
  if (actual.filterIsInstance<I>().isEmpty()) return
  expected("to NOT contain ${I::class.simpleName}, but contains ${show(actual.filterIsInstance<I>())}")
}
