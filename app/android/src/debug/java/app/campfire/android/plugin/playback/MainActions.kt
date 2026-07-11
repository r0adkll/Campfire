package app.campfire.android.plugin.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.livewire.ui.actions.ClickAction
import com.livewire.ui.actions.FloatValueChangeAction
import com.livewire.ui.actions.clickAction
import com.livewire.ui.actions.floatValueChangeAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [clickAction] variant that runs [block] on the main thread. MediaController and
 * ExoPlayer are main-thread confined while Livewire dispatches actions on its own
 * dispatcher, so every action that touches the player must go through one of these.
 */
@Composable
fun mainAction(key: Any? = null, block: () -> Unit): ClickAction {
  val scope = rememberCoroutineScope()
  return clickAction(key) {
    scope.launch(Dispatchers.Main.immediate) { block() }
  }
}

/**
 * [floatValueChangeAction] variant that runs [block] on the main thread.
 */
@Composable
fun mainFloatAction(block: (Float) -> Unit): FloatValueChangeAction {
  val scope = rememberCoroutineScope()
  return floatValueChangeAction { value ->
    scope.launch(Dispatchers.Main.immediate) { block(value) }
  }
}
