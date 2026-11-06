// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * The sum of both paddings, resolved when it is read rather than when it is built.
 *
 * Scaffold's content padding is backed by state that changes as insets animate (the navigation and
 * playback bars sliding with scroll). Summing eagerly reads that state during composition, so the
 * caller would recompose on every frame; deferring the read lets a lazy list pick the new values up
 * in its measure pass instead.
 */
operator fun PaddingValues.plus(paddingValues: PaddingValues): PaddingValues =
  SumPaddingValues(this, paddingValues)

/**
 * Equal whenever both operands are, so a caller that rebuilds the sum each composition still hands
 * its children an equal value.
 */
private data class SumPaddingValues(
  private val first: PaddingValues,
  private val second: PaddingValues,
) : PaddingValues {
  // Matches the eager version this replaced: both operands resolve as left-to-right, and the end
  // edge adds the second padding's start.
  private val start: Dp
    get() = first.calculateLeftPadding(LayoutDirection.Ltr) + second.calculateLeftPadding(LayoutDirection.Ltr)

  private val end: Dp
    get() = first.calculateRightPadding(LayoutDirection.Ltr) + second.calculateLeftPadding(LayoutDirection.Ltr)

  override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp =
    if (layoutDirection == LayoutDirection.Ltr) start else end

  override fun calculateTopPadding(): Dp = first.calculateTopPadding() + second.calculateTopPadding()

  override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp =
    if (layoutDirection == LayoutDirection.Ltr) end else start

  override fun calculateBottomPadding(): Dp = first.calculateBottomPadding() + second.calculateBottomPadding()
}
