// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.compose.runtime.compositionLocalOf

enum class SupportingContentState {
  Closed,
  Open,
}

val LocalSupportingContentState = compositionLocalOf {
  SupportingContentState.Closed
}
