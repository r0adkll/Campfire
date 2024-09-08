package app.campfire.core.util

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> createIfNotNull(vararg values: Any?, block: () -> R): R? {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }

  return if (values.all { it != null }) {
    block()
  } else {
    null
  }
}
