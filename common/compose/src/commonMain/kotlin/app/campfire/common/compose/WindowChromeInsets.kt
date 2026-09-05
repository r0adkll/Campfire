// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Insets the host window reserves that the platform does not report as system bars.
 *
 * Desktop has no status bar, so it provides a small top inset here to keep app bars from
 * sitting flush against the title bar; mobile platforms provide none. App bars consume it the
 * same way they consume the status bar inset, so it stays inside their background rather than
 * padding the content around them.
 */
val LocalWindowChromeInsets = staticCompositionLocalOf<WindowInsets> { WindowInsets(0) }
