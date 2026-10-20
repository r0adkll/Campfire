// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A small, dismissable card that surfaces app update state on builds that support in-app
 * updates (see [AppUpdates.isSupported]).
 *
 * - If an update is waiting, the card shows its version and headline with an Update action that
 *   opens the [AppUpdates.installSheet]. Tapping the card itself calls [onClick], to open the App
 *   updates settings. Dismissing it hides the card for that release until a different one is
 *   waiting.
 * - An update already downloading or ready to install stays on screen until installed.
 * - Otherwise, nothing is rendered.
 */
interface AppUpdateWidget {

  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  )
}
