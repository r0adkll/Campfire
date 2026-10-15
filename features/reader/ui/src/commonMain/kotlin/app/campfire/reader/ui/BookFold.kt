// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

/**
 * A vertical fold through the window, as on a foldable held open like a book: two pages read
 * best with the fold between them. [left] and [right] are its edges in window pixels.
 */
@Immutable
data class BookFold(val left: Float, val right: Float) {
  val center: Float get() = (left + right) / 2
}

/** The window's vertical fold while it divides the screen, or null. */
@Composable
internal fun currentBookFold(): BookFold? =
  currentWindowAdaptiveInfoV2().windowPosture.hingeList.bookFold()

internal fun List<HingeInfo>.bookFold(): BookFold? =
  firstOrNull { it.isVertical && (it.isSeparating || it.isOccluding || !it.isFlat) }
    ?.let { BookFold(it.bounds.left, it.bounds.right) }

/**
 * The gap, as a percentage of a page area [pageWidth] pixels wide, that keeps two pages clear
 * of [fold] with a margin either side. foliate-js centers its columns in the page area and,
 * until they reach their maximum width, spaces two of them by the gap's share of that width.
 */
internal fun foldGapPercent(fold: BookFold, pageWidth: Float, density: Density): Int {
  if (pageWidth <= 0f) return 0
  val needed = (fold.right - fold.left) + with(density) { FoldMargin.toPx() } * 2
  return ceil(needed / pageWidth * 100).toInt().coerceAtMost(MAX_GAP_PERCENT)
}

private val FoldMargin = 24.dp
private const val MAX_GAP_PERCENT = 30
