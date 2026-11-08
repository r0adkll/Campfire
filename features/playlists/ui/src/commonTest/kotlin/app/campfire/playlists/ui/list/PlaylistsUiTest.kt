// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.coroutines.LoadState
import app.campfire.core.settings.GroupDisplayState
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.sessions.api.QueuedEntry
import app.campfire.ui.appbar.CampfireAppBar
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
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
          campfireAppBar = CampfireAppBar { _, _ -> },
        )
      }
    }

    onNode(hasScrollAction()).performTouchInput { swipeDown() }
    waitForIdle()

    events.assertEvent(PlaylistsUiEvent.Refresh)
  }

  @Test
  fun theQueueShowsAsACardThatOpensIt() = runComposeUiTest {
    setPlaylists(GroupDisplayState.List, listOf(queued("The Queued Book")))

    onNodeWithText("Queue").assertIsDisplayed()
    onNodeWithText("Up next: The Queued Book").performClick()

    events.assertEvent(PlaylistsUiEvent.QueueClick)
  }

  @Test
  fun theQueueCardCountsItsEntriesInTheGrid() = runComposeUiTest {
    setPlaylists(GroupDisplayState.Grid, listOf(queued("One"), queued("Two")))

    onNodeWithText("2 items").assertIsDisplayed()
  }

  @Test
  fun anEmptyQueueHasNoCard() = runComposeUiTest {
    setPlaylists(GroupDisplayState.List, emptyList())

    onNodeWithText("Queue").assertDoesNotExist()
  }

  private fun ComposeUiTest.setPlaylists(displayState: GroupDisplayState, queue: List<QueuedEntry>) {
    setContent {
      PreviewSharedElementTransitionLayout {
        CompositionLocalProvider(
          LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
          LocalContentLayout provides ContentLayout.Root,
          LocalOverlayHost provides rememberOverlayHost(),
        ) {
          Playlists(
            state = PlaylistsUiState(
              playlistContentState = LoadState.Loaded(emptyList()),
              displayState = displayState,
              isRefreshing = false,
              queue = queue,
              eventSink = events::invoke,
            ),
            campfireAppBar = CampfireAppBar { _, _ -> },
          )
        }
      }
    }
  }

  private fun queued(title: String) = QueuedEntry(
    libraryItem(id = title, media = media(metadata = mediaMetadata(title = title))),
  )
}
