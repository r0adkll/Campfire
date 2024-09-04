package app.campfire.shared.navigator

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import app.campfire.shared.navigator.NavigationType.BOTTOM_NAVIGATION
import app.campfire.shared.navigator.NavigationType.RAIL

/**
 * Return if the supporting pane layout is enabled for this size class
 */
val WindowSizeClass.isSupportingPaneEnabled: Boolean
  get() = widthSizeClass >= WindowWidthSizeClass.Medium &&
    heightSizeClass >= WindowHeightSizeClass.Medium

/**
 * Return the main navigation type for this size class
 */
val WindowSizeClass.navigationType: NavigationType
  get() = when {
    widthSizeClass == WindowWidthSizeClass.Compact -> BOTTOM_NAVIGATION
    // TODO: This is essentially a phone portrait mode, What would be the optimal setup for this
//      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> BOTTOM_NAVIGATION
    widthSizeClass == WindowWidthSizeClass.Medium -> RAIL
    else -> RAIL
  }
