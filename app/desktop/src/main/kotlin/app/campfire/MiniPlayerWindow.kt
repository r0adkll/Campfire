// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowState
import app.campfire.common.root.MiniPlayerContent
import app.campfire.core.model.LibraryItemId
import java.awt.Dimension

/**
 * The player lifted out into its own window: borderless, translucent-cornered, always above other
 * windows, and sized between a minimum that still fits the player's compact form and a maximum
 * that keeps it a companion rather than a second main window. Its chrome doubles as the drag
 * handle, since there is no title bar; the edges still resize. Escape closes it.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ApplicationScope.MiniPlayerWindow(
  state: WindowState,
  icon: Painter,
  content: MiniPlayerContent,
  onClose: () -> Unit,
  onItemClick: (LibraryItemId) -> Unit,
) {
  Window(
    onCloseRequest = onClose,
    state = state,
    title = "Campfire",
    icon = icon,
    decoration = WindowDecoration.Undecorated(),
    transparent = true,
    resizable = true,
    alwaysOnTop = true,
    onKeyEvent = { event ->
      if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
        onClose()
        true
      } else {
        false
      }
    },
  ) {
    // Compose's WindowState carries no bounds, so both limits go on the AWT window itself.
    LaunchedEffect(window) {
      window.minimumSize = Dimension(MiniPlayerMinWidth, MiniPlayerMinHeight)
      window.maximumSize = Dimension(MiniPlayerMaxWidth, MiniPlayerMaxHeight)
    }

    content(
      onItemClick,
      onClose,
      onClose,
      { topBar -> WindowDraggableArea { topBar() } },
      Modifier.fillMaxSize(),
    )
  }
}

/** The mini-player's opening size, in dp. Tall enough for the full stacked layout. */
const val MiniPlayerDefaultWidth = 400
const val MiniPlayerDefaultHeight = 680

/** The smallest square the compact player reads well in. */
private const val MiniPlayerMinWidth = 360
private const val MiniPlayerMinHeight = 360

/** Above this it stops being a mini-player. */
private const val MiniPlayerMaxWidth = 600
private const val MiniPlayerMaxHeight = 960
