package app.campfire.common.compose.extensions

import androidx.compose.ui.Modifier
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun Modifier.thenIf(
  condition: Boolean,
  whenFalse: Modifier.() -> Modifier = { this },
  whenTrue: Modifier.() -> Modifier,
): Modifier = if (condition) whenTrue() else whenFalse()

@OptIn(ExperimentalContracts::class)
inline fun <T> Modifier.thenIfNotNull(
  subject: T?,
  block: Modifier.(T) -> Modifier,
): Modifier {
  contract {
    callsInPlace(block, InvocationKind.AT_MOST_ONCE)
  }

  return if (subject != null) {
    block(subject)
  } else {
    this
  }
}
