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
  Expanded,
}

data class WidgetSizeClass(
  val widthSizeClass: WidgetWidthClass,
  val heightSizeClass: WidgetHeightClass,
) {
  companion object {

    // Responsive sizes that define distinct layout breakpoints.
    // Glance will pick the largest size from this set that fits the actual widget bounds.
    // Each size corresponds to a specific layout variant.
    val SINGLE_SINGLE = DpSize(60.dp, 60.dp)
    val COMPACT_SINGLE = DpSize(200.dp, 60.dp)
    val EXPANDED_SINGLE = DpSize(340.dp, 60.dp)
    val EXPANDED_TALL = DpSize(340.dp, 200.dp)

    val ResponsiveSizes = setOf(
      SINGLE_SINGLE,
      COMPACT_SINGLE,
      EXPANDED_SINGLE,
      EXPANDED_TALL,
    )

    fun from(size: DpSize): WidgetSizeClass = when (size) {
      EXPANDED_TALL -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Expanded)
      EXPANDED_SINGLE -> WidgetSizeClass(WidgetWidthClass.Expanded, WidgetHeightClass.Single)
      COMPACT_SINGLE -> WidgetSizeClass(WidgetWidthClass.Compact, WidgetHeightClass.Single)
      SINGLE_SINGLE -> WidgetSizeClass(WidgetWidthClass.Single, WidgetHeightClass.Single)
      // Fallback for any unexpected size
      else -> WidgetSizeClass(
        widthSizeClass = when {
          size.width >= 340.dp -> WidgetWidthClass.Expanded
          size.width >= 200.dp -> WidgetWidthClass.Compact
          else -> WidgetWidthClass.Single
        },
        heightSizeClass = when {
          size.height >= 200.dp -> WidgetHeightClass.Expanded
          else -> WidgetHeightClass.Single
        },
      )
    }
  }
}
