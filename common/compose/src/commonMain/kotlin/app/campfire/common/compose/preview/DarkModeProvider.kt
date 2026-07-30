// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.preview

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

class DarkModeProvider : PreviewParameterProvider<Boolean> {
  override val values: Sequence<Boolean> = sequenceOf(true, false)
}
