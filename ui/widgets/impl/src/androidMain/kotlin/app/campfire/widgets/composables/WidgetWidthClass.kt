// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.widgets.composables

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

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

    // The layout breakpoints. The widget is composed at its exact size (see PlayerWidget.sizeMode)
    // and lays out for the breakpoint that best fits it, picked the way Glance's responsive mode
    // would. Each size corresponds to a specific layout variant.
    val SINGLE_SINGLE = DpSize(60.dp, 60.dp)
    val COMPACT_SINGLE = DpSize(200.dp, 60.dp)
    val EXPANDED_SINGLE = DpSize(340.dp, 60.dp)
    val EXPANDED_COMPACT = DpSize(340.dp, 140.dp)
    val EXPANDED_LARGE_COMPACT = DpSize(340.dp, 210.dp)
    val EXPANDED_EXPANDED = DpSize(340.dp, 340.dp)
    val EXPANDED_TALL = DpSize(340.dp, 480.dp)
    val EXPANDED_EXTRA_TALL = DpSize(340.dp, 620.dp)

    val Breakpoints = setOf(
      SINGLE_SINGLE,
      COMPACT_SINGLE,
      EXPANDED_SINGLE,
      EXPANDED_COMPACT,
      EXPANDED_LARGE_COMPACT,
      EXPANDED_EXPANDED,
      EXPANDED_TALL,
      EXPANDED_EXTRA_TALL,
    )

    /**
     * The size class for a widget measuring [size]: that of the breakpoint closest to it among
     * those that fit, or of the smallest breakpoint when none does. This is how Glance picks among
     * the sizes of a [androidx.glance.appwidget.SizeMode.Responsive] widget, so layouts are the
     * same as when this widget declared its breakpoints that way.
     */
    fun from(size: DpSize): WidgetSizeClass = when (bestBreakpoint(size)) {
      EXPANDED_EXTRA_TALL -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.ExtraTall)
      EXPANDED_TALL -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Tall)
      EXPANDED_EXPANDED -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Expanded)
      EXPANDED_LARGE_COMPACT -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.LargeCompact)
      EXPANDED_COMPACT -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Compact)
      EXPANDED_SINGLE -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Single)
      COMPACT_SINGLE -> WidgetSizeClass(WidgetWidthClass.Compact, WidgetHeightClass.Single)
      else -> WidgetSizeClass(WidgetWidthClass.Single, WidgetHeightClass.Single)
    }

    internal fun bestBreakpoint(size: DpSize): DpSize = Breakpoints
      .filter { it fitsIn size }
      .minByOrNull { squareDistance(size, it) }
      ?: SINGLE_SINGLE

    // Glance's own tolerance: launchers report sizes in fractional dp
    private infix fun DpSize.fitsIn(available: DpSize): Boolean =
      ceil(available.width.value) + 1 > width.value && ceil(available.height.value) + 1 > height.value

    private fun squareDistance(a: DpSize, b: DpSize): Float {
      val dw = a.width.value - b.width.value
      val dh = a.height.value - b.height.value
      return dw * dw + dh * dh
    }
  }
}
