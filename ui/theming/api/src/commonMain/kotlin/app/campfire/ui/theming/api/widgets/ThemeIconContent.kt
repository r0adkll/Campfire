// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ThemeIconContent {

  // No default values: since Kotlin 2.4.20, calling an interface composable's default from another
  // module fails to link on Kotlin/Native (CMP-9392)
  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  )
}
