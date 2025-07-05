package app.campfire.widgets.composables

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

enum class WidgetWidthClass(val breakpoint: Dp) {
  Single(90.dp),
  Compact(250.dp),
  Expanded(380.dp),
}

enum class WidgetHeightClass(val breakpoint: Dp) {
  Single(120.dp),
  Compact(250.dp),
  Expanded(290.dp),
}

data class WidgetSizeClass(
  val widthSizeClass: WidgetWidthClass,
  val heightSizeClass: WidgetHeightClass,
) {
  companion object {
    fun from(size: DpSize): WidgetSizeClass {
      val widthClass = WidgetWidthClass.entries
        .firstOrNull { size.width < it.breakpoint }
        ?: WidgetWidthClass.Expanded
      val heightClass = WidgetHeightClass.entries
        .firstOrNull { size.height < it.breakpoint }
        ?: WidgetHeightClass.Expanded
      return WidgetSizeClass(widthClass, heightClass)
    }
  }
}
