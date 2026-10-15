// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.window

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import app.campfire.common.screens.WindowScreen

/** Opens a [WindowScreen] in a window of its own. */
fun interface WindowScreenLauncher {
  fun launch(screen: WindowScreen)
}

/**
 * The [WindowScreenLauncher] for the app's window, or null on platforms that have no separate
 * window to open, where window screens are pushed like any other screen.
 */
val LocalWindowScreenLauncher = staticCompositionLocalOf<WindowScreenLauncher?> { null }

/** The platform window a [WindowScreen] is shown in. */
interface WindowScreenController {

  /**
   * Show or hide the system bars: the status and navigation bars on Android, the status bar on
   * iOS. Hidden bars come back for a moment on a swipe from the screen's edge.
   */
  fun setSystemBarsVisible(visible: Boolean)

  /** Draw the system bars' icons dark, for something light behind them, or light. */
  fun setLightSystemBars(light: Boolean)

  /** Keep the screen from dimming and locking while this window is in front. */
  fun setKeepScreenOn(keepOn: Boolean)

  /** Whether the platform hands the volume keys to the app at all. */
  val supportsVolumeKeys: Boolean

  /**
   * Take the volume keys while [handler] is set: it gets true for volume up, false for down, and
   * returns whether it used the key. A key it doesn't use changes the volume as usual.
   */
  fun setVolumeKeyHandler(handler: ((up: Boolean) -> Boolean)?)
}

/** The window the current [WindowScreen] is shown in, or null when it isn't in one of its own. */
val LocalWindowScreenController = staticCompositionLocalOf<WindowScreenController?> { null }

/** Show the system bars only while [visible], restoring them when this leaves the composition. */
@Composable
fun SystemBarsVisibleEffect(visible: Boolean) {
  val controller = LocalWindowScreenController.current ?: return
  DisposableEffect(controller, visible) {
    controller.setSystemBarsVisible(visible)
    onDispose { controller.setSystemBarsVisible(true) }
  }
}

/** Keep the screen on while [keepOn] and this is in the composition. */
@Composable
fun KeepScreenOnEffect(keepOn: Boolean) {
  val controller = LocalWindowScreenController.current ?: return
  DisposableEffect(controller, keepOn) {
    controller.setKeepScreenOn(keepOn)
    onDispose { controller.setKeepScreenOn(false) }
  }
}

/** Draw the system bars' icons for a light ([light]) or dark background while this is composed. */
@Composable
fun LightSystemBarsEffect(light: Boolean) {
  val controller = LocalWindowScreenController.current ?: return
  DisposableEffect(controller, light) {
    controller.setLightSystemBars(light)
    onDispose { }
  }
}

/** Hand the volume keys to [onKey] while [enabled] and this is composed. */
@Composable
fun VolumeKeysEffect(enabled: Boolean, onKey: (up: Boolean) -> Boolean) {
  val controller = LocalWindowScreenController.current ?: return
  val currentOnKey by rememberUpdatedState(onKey)
  DisposableEffect(controller, enabled) {
    controller.setVolumeKeyHandler(if (enabled) { up -> currentOnKey(up) } else null)
    onDispose { controller.setVolumeKeyHandler(null) }
  }
}
