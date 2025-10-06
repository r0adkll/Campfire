package app.campfire.core.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

class CoroutineScopeHolder(val initializer: () -> CoroutineScope) {
  private var coroutineScope: CoroutineScope = initializer()

  fun get(): CoroutineScope {
    if (!coroutineScope.isActive) {
      coroutineScope = initializer()
    }
    return coroutineScope
  }

  fun cancel(reason: String) {
    coroutineScope.cancel(reason)
  }
}
