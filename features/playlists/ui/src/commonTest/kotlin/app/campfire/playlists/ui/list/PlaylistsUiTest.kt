// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.coroutines.LoadState
import app.campfire.core.settings.GroupDisplayState
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PlaylistsUiTest {

  private val events = TestEventSink<PlaylistsUiEvent>()

  @Test
  fun pullingDownThePlaylistsRefreshesThem() = runComposeUiTest {
    setContent {
      // Phone-sized, so the insets resolve the way they do on a handset
      CompositionLocalProvider(
        LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
        LocalContentLayout provides ContentLayout.Root,
        LocalOverlayHost provides rememberOverlayHost(),
      ) {
        Playlists(
          state = PlaylistsUiState(
            playlistContentState = LoadState.Loaded(emptyList()),
            displayState = GroupDisplayState.List,
            isRefreshing = false,
            eventSink = events::invoke,
          ),
          campfireAppBar = { _, _ -> },
        )
      }
    }

    onNode(hasScrollAction()).performTouchInput { swipeDown() }
    waitForIdle()

    events.assertEvent(PlaylistsUiEvent.Refresh)
  }
}
