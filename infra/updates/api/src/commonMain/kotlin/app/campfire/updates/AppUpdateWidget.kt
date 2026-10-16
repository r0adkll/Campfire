// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A small, dismissable widget that surfaces app update state on builds that support
 * in-app updates (see [app.campfire.updates.source.AppUpdateSource.isSupported]).
 *
 * - If an update is available, a compact card is shown that opens a bottom sheet with
 *   the full release details, download action, and download progress (the source's own
 *   sheet when it has one). Dismissing it hides the card for that release until a
 *   different one becomes available.
 * - An update already downloading or ready to install stays on screen until installed.
 * - Otherwise, nothing is rendered.
 */
interface AppUpdateWidget {

  @Composable
  fun Content(modifier: Modifier)
}
