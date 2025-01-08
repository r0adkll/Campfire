package app.campfire.common.compose

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.layout.isSupportingPaneEnabled
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

    // Inset content if not extra-large screen, the playback session is live, and this content is not in the
    // supporting pane.
    val isExtraLargeScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.ExtraLarge
    val playbackBarInsets = if (!isExtraLargeScreen && session != null && contentLayout == ContentLayout.Root) {
      WindowInsets(bottom = PlaybackBarInsetSize)
    } else {
      WindowInsets(0.dp)
    }

    return if (windowSizeClass.isSupportingPaneEnabled) {
      WindowInsets.systemBars.exclude(WindowInsets.statusBars).add(playbackBarInsets)
    } else {
      ScaffoldDefaults.contentWindowInsets.add(playbackBarInsets)
    }
  }

private val PlaybackBarInsetSize = 56.dp + 16.dp
