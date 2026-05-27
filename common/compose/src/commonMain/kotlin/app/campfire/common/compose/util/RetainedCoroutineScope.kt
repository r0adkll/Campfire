package app.campfire.common.compose.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import com.slack.circuit.retained.rememberRetained
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

@Composable
fun rememberRetainedCoroutineScope(
  vararg inputs: Any? = arrayOf("coroutine_scope"),
): CoroutineScope {
  val retainedId = rememberRetained {
    Uuid.random().toHexDashString().takeLast(5)
  }
  return rememberRetained(*inputs) {
    object : RememberObserver {
      val scope = CoroutineScope(
        Dispatchers.Main + Job() +
          CoroutineName(inputs.joinToString { it.toString() } + " ID[$retainedId]"),
      )

      override fun onForgotten() {
        // We've been forgotten, cancel the CoroutineScope
        scope.cancel()
      }

      // Not called by Circuit
      override fun onAbandoned() = Unit

      // Nothing to do here
      override fun onRemembered() = Unit
    }
  }.scope
}
