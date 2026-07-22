package app.campfire.common.back

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner

/**
 * A back handler that registers with the [NavigationEventDispatcher] at
 * [NavigationEventDispatcher.PRIORITY_OVERLAY].
 *
 * The convenience `NavigationBackHandler` composable always registers at
 * `PRIORITY_DEFAULT`, which is the same tier Circuit's `GestureNavigationDecoration` uses for
 * popping the main back stack. Within a tier the dispatcher picks the winner purely by
 * composition order (LIFO), so whichever handler happens to compose last wins — a source of
 * non-deterministic back ordering.
 *
 * All `PRIORITY_OVERLAY` handlers are dispatched before *any* `PRIORITY_DEFAULT` handler,
 * regardless of composition order. Use this for "chrome" back consumers (overlays, an expanded
 * player, a supporting/detail pane) that must always take precedence over a main back stack pop,
 * while letting Circuit keep its predictive-back screen animation for the pop itself.
 *
 * When [enabled] is `false` the handler is skipped and the event falls through to the next enabled
 * handler (e.g. Circuit's default-priority pop).
 *
 * This intentionally does not feed predictive-back progress; it mirrors the previous
 * `NavigationBackHandler(state = rememberNavigationEventState(NavigationEventInfo.None), ...)`
 * usage which also opted out of a predictive preview.
 */
@Composable
fun OverlayPriorityBackHandler(
  enabled: Boolean,
  onBack: () -> Unit,
) {
  val dispatcher = LocalNavigationEventDispatcherOwner.current
    ?.navigationEventDispatcher
    ?: return

  val currentOnBack by rememberUpdatedState(onBack)

  val handler = remember {
    object : NavigationEventHandler<NavigationEventInfo>(NavigationEventInfo.None, enabled) {
      override fun onBackCompleted() {
        currentOnBack()
      }
    }
  }

  SideEffect {
    handler.isBackEnabled = enabled
  }

  DisposableEffect(dispatcher) {
    dispatcher.addHandler(handler, NavigationEventDispatcher.PRIORITY_OVERLAY)
    onDispose {
      handler.remove()
    }
  }
}
