// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.layout

import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

/**
 * Lower bound, in dp, of the Large width size class (desktop and web windows). Mirrors the
 * value material3-adaptive uses when large and extra-large widths are enabled.
 */
const val WIDTH_DP_LARGE_LOWER_BOUND: Int = 1200

/**
 * Lower bound, in dp, of the Extra-Large width size class (ultra-wide desktop and web windows).
 */
const val WIDTH_DP_EXTRA_LARGE_LOWER_BOUND: Int = 1600

/** Width is at least the Medium breakpoint (tablets in portrait, unfolded inner displays). */
val WindowSizeClass.isWidthAtLeastMedium: Boolean
  get() = isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)

/** Width is at least the Expanded breakpoint (tablets in landscape). */
val WindowSizeClass.isWidthAtLeastExpanded: Boolean
  get() = isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)

/** Width is at least the Large breakpoint (desktop and web windows). */
val WindowSizeClass.isWidthAtLeastLarge: Boolean
  get() = isWidthAtLeastBreakpoint(WIDTH_DP_LARGE_LOWER_BOUND)

/** Width is at least the Extra-Large breakpoint (ultra-wide desktop and web windows). */
val WindowSizeClass.isWidthAtLeastExtraLarge: Boolean
  get() = isWidthAtLeastBreakpoint(WIDTH_DP_EXTRA_LARGE_LOWER_BOUND)

/** Height is below the Medium breakpoint (the majority of phones in landscape). */
val WindowSizeClass.isHeightCompact: Boolean
  get() = !isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)

/**
 * Return if the supporting pane layout is enabled for this size class
 */
val WindowSizeClass.isSupportingPaneEnabled: Boolean
  get() = isWidthAtLeastMedium

/**
 * Return if the device is in landscape/phone mode
 */
val WindowSizeClass.isLandscapePhone: Boolean
  get() = isWidthAtLeastExpanded && isHeightCompact

/**
 * Return the main navigation type for this size class
 */
val WindowSizeClass.navigationType: NavigationType
  get() = when {
    isWidthAtLeastExtraLarge -> NavigationType.Drawer
    isWidthAtLeastMedium -> NavigationType.Rail
    // TODO: This is essentially a phone portrait mode, What would be the optimal setup for this
    else -> NavigationType.BottomNavigation
  }
