package app.campfire.common.compose.layout

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass

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
  get() = when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> NavigationType.BottomNavigation
    // TODO: This is essentially a phone portrait mode, What would be the optimal setup for this
//      windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact -> BOTTOM_NAVIGATION
    WindowWidthSizeClass.Medium -> NavigationType.Rail
    WindowWidthSizeClass.Expanded -> NavigationType.Rail
    WindowWidthSizeClass.Large -> NavigationType.Rail
    WindowWidthSizeClass.ExtraLarge -> NavigationType.Drawer
    else -> NavigationType.Rail
  }
