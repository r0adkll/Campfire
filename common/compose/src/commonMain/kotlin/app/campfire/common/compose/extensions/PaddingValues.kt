// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection

operator fun PaddingValues.plus(paddingValues: PaddingValues): PaddingValues {
  return PaddingValues(
    start = calculateStartPadding(LayoutDirection.Ltr) +
      paddingValues.calculateStartPadding(LayoutDirection.Ltr),
    top = calculateTopPadding() + paddingValues.calculateTopPadding(),
    end = calculateEndPadding(LayoutDirection.Ltr) +
      paddingValues.calculateStartPadding(LayoutDirection.Ltr),
    bottom = calculateBottomPadding() + paddingValues.calculateBottomPadding(),
  )
}
