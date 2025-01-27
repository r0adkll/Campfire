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
