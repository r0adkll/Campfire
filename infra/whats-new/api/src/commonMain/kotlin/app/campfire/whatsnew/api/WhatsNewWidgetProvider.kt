// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.whatsnew.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

interface WhatsNewWidgetProvider {

  @Composable
  fun Content(
    onClick: () -> Unit,
    modifier: Modifier,
  )
}
