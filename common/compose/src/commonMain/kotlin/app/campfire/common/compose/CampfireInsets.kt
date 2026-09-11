// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
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

    // Union rather than add, so a cutout that overlaps the system bars is only counted once.
    return ScaffoldDefaults.contentWindowInsets
      .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
      .add(playbackBarInsets)
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
