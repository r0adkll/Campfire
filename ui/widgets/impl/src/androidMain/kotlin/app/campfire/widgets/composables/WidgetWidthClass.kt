// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WidgetWidthClass {
  Single,
  Compact,
  Expanded,
}

enum class WidgetHeightClass {
  Single,
  Compact,
  LargeCompact,
  Expanded,
  Tall,
  ExtraTall,
}

data class WidgetSizeClass(
  val width: WidgetWidthClass,
  val height: WidgetHeightClass,
) {
  companion object {

    // Responsive sizes that define distinct layout breakpoints.
    // Glance will pick the largest size from this set that fits the actual widget bounds.
    // Each size corresponds to a specific layout variant.
    val SINGLE_SINGLE = DpSize(60.dp, 60.dp)
    val COMPACT_SINGLE = DpSize(200.dp, 60.dp)
    val EXPANDED_SINGLE = DpSize(340.dp, 60.dp)
    val EXPANDED_COMPACT = DpSize(340.dp, 140.dp)
    val EXPANDED_LARGE_COMPACT = DpSize(340.dp, 210.dp)
    val EXPANDED_EXPANDED = DpSize(340.dp, 340.dp)
    val EXPANDED_TALL = DpSize(340.dp, 480.dp)
    val EXPANDED_EXTRA_TALL = DpSize(340.dp, 620.dp)

    val ResponsiveSizes = setOf(
      SINGLE_SINGLE,
      COMPACT_SINGLE,
      EXPANDED_SINGLE,
      EXPANDED_COMPACT,
      EXPANDED_LARGE_COMPACT,
      EXPANDED_EXPANDED,
      EXPANDED_TALL,
      EXPANDED_EXTRA_TALL,
    )

    fun from(size: DpSize): WidgetSizeClass = when (size) {
      EXPANDED_EXTRA_TALL -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.ExtraTall)
      EXPANDED_TALL -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Tall)
      EXPANDED_EXPANDED -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Expanded)
      EXPANDED_COMPACT -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Compact)
      EXPANDED_LARGE_COMPACT -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.LargeCompact)
      EXPANDED_SINGLE -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Single)
      COMPACT_SINGLE -> WidgetSizeClass(WidgetWidthClass.Compact, WidgetHeightClass.Single)
      SINGLE_SINGLE -> WidgetSizeClass(WidgetWidthClass.Single, WidgetHeightClass.Single)
      // Fallback for any unexpected size
      else -> WidgetSizeClass(
        width = when {
          size.width >= 340.dp -> WidgetWidthClass.Expanded
          size.width >= 200.dp -> WidgetWidthClass.Compact
          else -> WidgetWidthClass.Single
        },
        height = when {
          size.height >= 340.dp -> WidgetHeightClass.Expanded
          size.height >= 200.dp -> WidgetHeightClass.Compact
          else -> WidgetHeightClass.Single
        },
      )
    }
  }
}
