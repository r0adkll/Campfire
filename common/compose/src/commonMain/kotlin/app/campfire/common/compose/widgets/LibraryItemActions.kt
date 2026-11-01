// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.core.model.LibraryItem
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.launch

/**
 * What can be done to a library item wherever it's listed, offered on a long press of its card or
 * a right click on it. Provided by the signed-in app; previews and tests have none, so their cards
 * offer nothing.
 */
interface LibraryItemActions {

  /**
   * Whether the signed-in account can do anything to [item] at all. Read in composition, so a card
   * follows the account's permissions as they change.
   */
  @Composable
  fun canAct(item: LibraryItem): Boolean

  /**
   * Show what can be done to [item] over [overlayHost]: as a menu at [pointer], a position in the
   * window, for a right click, or as a sheet. Outcomes worth telling are told in [snackbarHost],
   * after the overlay has gone.
   */
  suspend fun show(
    item: LibraryItem,
    pointer: Offset?,
    overlayHost: OverlayHost,
    snackbarHost: SnackbarHostState,
  )
}

/** Makes the [LibraryItemActions] that go to their screens with [navigator], the app's own. */
fun interface LibraryItemActionsFactory {
  fun create(navigator: Navigator): LibraryItemActions
}

val LocalLibraryItemActions = staticCompositionLocalOf<LibraryItemActions?> { null }

/**
 * Opens [item]'s actions, with the pointer's window position for a right click and null for a long
 * press, or null when the signed-in account can't do anything to it.
 */
@Composable
fun rememberLibraryItemActionsOpener(item: LibraryItem): ((pointer: Offset?) -> Unit)? {
  val actions = LocalLibraryItemActions.current ?: return null
  if (!actions.canAct(item)) return null

  val overlayHost = LocalOverlayHost.current
  val snackbarHost = LocalSnackBarHost.current
  val scope = rememberCoroutineScope()
  val currentItem by rememberUpdatedState(item)
  return remember(actions, overlayHost, snackbarHost, scope) {
    { pointer -> scope.launch { actions.show(currentItem, pointer, overlayHost, snackbarHost) } }
  }
}

/**
 * Calls [onRightClick] with the pointer's position in the window when a mouse's secondary button
 * presses on this, for a menu to open where it was clicked.
 */
@Composable
fun Modifier.onRightClick(onRightClick: ((windowPosition: Offset) -> Unit)?): Modifier {
  if (onRightClick == null) return this
  val currentOnRightClick by rememberUpdatedState(onRightClick)
  val target = remember { RightClickTarget() }
  return this
    .onGloballyPositioned { target.coordinates = it }
    .pointerInput(Unit) {
      awaitPointerEventScope {
        while (true) {
          val event = awaitPointerEvent(PointerEventPass.Initial)
          if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
            val change = event.changes.first()
            val coordinates = target.coordinates ?: continue
            // Taken, so the press doesn't also count as a click
            change.consume()
            currentOnRightClick(coordinates.localToWindow(change.position))
          }
        }
      }
    }
}

private class RightClickTarget {
  var coordinates: LayoutCoordinates? = null
}
