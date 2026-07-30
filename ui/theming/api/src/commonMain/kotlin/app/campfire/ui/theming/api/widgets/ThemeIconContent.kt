// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.api.widgets

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface ThemeIconContent {

  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
  )
}
