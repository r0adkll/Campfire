// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.window.core.layout.WindowSizeClass

val LocalWindowSizeClass = staticCompositionLocalOf<WindowSizeClass> {
  error("No WindowSizeClass available")
}

/**
 * The [WindowSizeClass] of the current window, computed with the Large (1200dp) and
 * Extra-Large (1600dp) width breakpoints enabled so wide desktop windows are distinguishable.
 */
@Composable
fun currentWindowSizeClass(): WindowSizeClass =
  currentWindowAdaptiveInfoV2().windowSizeClass
