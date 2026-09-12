// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.theme

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
actual fun ApplyStatusBar(useDarkColors: Boolean) {
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).apply {
        // Both bars follow the in-app theme rather than the system's night mode, which
        // enableEdgeToEdge() defaults to. They diverge whenever the user picks a theme that
        // doesn't match the system, and a mismatched navigation bar leaves the gesture handle
        // invisible against the app's background.
        isAppearanceLightStatusBars = !useDarkColors
        isAppearanceLightNavigationBars = !useDarkColors
      }
    }
  }
}
