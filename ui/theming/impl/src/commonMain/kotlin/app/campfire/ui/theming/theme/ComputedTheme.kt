// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.theme

import com.r0adkll.swatchbuckler.compose.Theme

data class ComputedTheme(
  val key: String,
  val cacheKey: String,
  val theme: Theme,
)
