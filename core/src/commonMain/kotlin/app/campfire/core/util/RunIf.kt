package app.campfire.core.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * Run the given [block] ONLY if the passed [predicate] is true
 */
@OptIn(ExperimentalContracts::class)
inline fun runIf(predicate: Boolean, block: () -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }

  if (predicate) block()
}

@OptIn(ExperimentalContracts::class)
inline fun <A, B, C> runIfNotNull(
  value1: A?,
  value2: B?,
  value3: C?,
  block: (A, B, C) -> Unit,
) {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }

  if (value1 != null && value2 != null && value3 != null) {
    block(value1, value2, value3)
  }
}

@OptIn(ExperimentalContracts::class)
inline fun runIfAllNotNull(vararg values: Any?, block: (Array<Any>) -> Unit) {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }

  if (values.all { it != null }) {
    @Suppress("UNCHECKED_CAST")
    block(values as Array<Any>)
  }
}
