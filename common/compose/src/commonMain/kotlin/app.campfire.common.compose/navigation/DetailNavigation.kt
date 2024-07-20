package app.campfire.common.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import app.campfire.common.screens.BaseScreen

sealed interface DetailNavigation {
  /**
   * This represents that the current device is compact and will
   * never have a main-detail screen pattern
   */
  data object None : DetailNavigation

  /**
   * This represents that the current device supports a main-detail pattern
   * but doesn't have a detail screen shown at the moment
   */
  data object Hidden : DetailNavigation

  /**
   * This represents that the current device supports a main-detail pattern
   * AND is showing a detail screen at this time
   */
  data class Current(val screen: BaseScreen) : DetailNavigation
}

/**
 * Composition local that screens can use to determine if they are in a panel detail
 * CircuitContent and should thus change its UI treatments accordingly
 */
val LocalDetailNavigation = compositionLocalOf<DetailNavigation> {
  DetailNavigation.None
}

@Composable
fun isInDetailMode(): Boolean {
  return LocalDetailNavigation.current is DetailNavigation.Current
}
