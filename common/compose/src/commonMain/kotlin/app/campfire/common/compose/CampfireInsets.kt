// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.LocalSupportingContentState
import app.campfire.common.compose.layout.NavigationType
import app.campfire.common.compose.layout.SupportingContentState
import app.campfire.common.compose.layout.navigationType
import app.campfire.common.compose.layout.usesBottomPlaybackBar
import app.campfire.common.compose.session.LocalPlaybackSession

/**
 * Common set of window insets to use for Campfire scaffolds to adjust according to
 * display size and expected layouts as well as compensate for the visibility of
 * the session playback bar.
 */
val CampfireWindowInsets: WindowInsets
  @Composable get() {
    val windowSizeClass = LocalWindowSizeClass.current
    val session by rememberUpdatedState(LocalPlaybackSession.current)
    val contentLayout = LocalContentLayout.current

    // Inset content if the playback bar floats over it (rather than docking below it), the playback
    // session is live, and this content is not in the supporting pane.
    val floatingPlaybackBar = !windowSizeClass.usesBottomPlaybackBar
    val playbackBarInsets = if (floatingPlaybackBar && session != null && contentLayout == ContentLayout.Root) {
      WindowInsets(bottom = PlaybackBarInsetSize)
    } else {
      WindowInsets(0.dp)
    }

    return ScaffoldDefaults.contentWindowInsets
      .only(WindowInsetsSides.Vertical)
      .add(HorizontalSafeInsets)
      .add(playbackBarInsets)
  }

/**
 * The horizontal safe-area insets the calling content is responsible for.
 *
 * Only whatever actually touches a window edge should inset for it. The navigation rail or
 * permanent drawer owns the leading edge whenever one is shown, the supporting pane owns the
 * trailing edge whenever it is open, and the root content owns whichever edge neither covers.
 * Applying the window's full horizontal insets everywhere instead leaves a gap between the root
 * content and the pane beside it, and pads the pane away from an edge it never touches.
 */
internal val HorizontalSafeInsets: WindowInsets
  @Composable get() {
    val sides = when (LocalContentLayout.current) {
      // The pane hangs off the trailing edge, with the root content and rail between it and the
      // leading one.
      ContentLayout.Supporting -> WindowInsetsSides.End
      ContentLayout.Root -> {
        val navigationOwnsStart =
          LocalWindowSizeClass.current.navigationType != NavigationType.BottomNavigation
        val paneOwnsEnd = LocalSupportingContentState.current == SupportingContentState.Open
        when {
          navigationOwnsStart && paneOwnsEnd -> return WindowInsets(0.dp)
          navigationOwnsStart -> WindowInsetsSides.End
          paneOwnsEnd -> WindowInsetsSides.Start
          else -> WindowInsetsSides.Horizontal
        }
      }
    }

    // Union rather than add, so a cutout that overlaps the system bars is only counted once.
    return WindowInsets.systemBars
      .union(WindowInsets.displayCutout)
      .only(sides)
  }

/**
 * The bottom edge of the system navigation bar, which the in-app navigation bar already draws over.
 *
 * Screens that sit behind the in-app bar exclude this from their content insets so the two don't
 * stack. Excluding [WindowInsets.navigationBars] outright would also drop the *side* inset the
 * system bar takes in landscape with three-button navigation, leaving content under it.
 */
val OverlappedNavigationBarInsets: WindowInsets
  @Composable get() = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)

private val PlaybackBarInsetSize = 56.dp + 32.dp
