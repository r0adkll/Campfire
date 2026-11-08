// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.queue

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.window.core.layout.WindowSizeClass
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.QueueUiState
import assertk.assertThat
import assertk.assertions.containsExactly
import com.slack.circuit.test.TestEventSink
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class QueueUiTest {

  private val screenEvents = TestEventSink<QueueScreenUiEvent>()
  private val queueEvents = mutableListOf<QueueUiEvent>()

  private val playing = entry("playing", "The Playing Book")
  private val first = entry("first", "The First Book")
  private val second = entry("second", "The Second Book")

  @Test
  fun `an empty queue with nothing playing says how to add to it`() = runComposeUiTest {
    setQueue(nowPlaying = null, queue = emptyList())

    onNodeWithText(EMPTY_MESSAGE, substring = true).assertIsDisplayed()
    onNodeWithContentDescription("Play queue").assertDoesNotExist()
    onNodeWithContentDescription("Clear queue").assertDoesNotExist()
  }

  @Test
  fun `with nothing playing the queue can be played from the top`() = runComposeUiTest {
    setQueue(nowPlaying = null, queue = listOf(first, second))

    onNodeWithText("The First Book").assertIsDisplayed()
    onNodeWithText("The Second Book").assertIsDisplayed()
    onNodeWithContentDescription("Play queue").performClick()

    screenEvents.assertEvent(QueueScreenUiEvent.PlayQueue)
  }

  @Test
  fun `what's playing shows above an empty queue`() = runComposeUiTest {
    setQueue(nowPlaying = playing, queue = emptyList())

    onNodeWithText("Now playing").assertIsDisplayed()
    onNodeWithText(EMPTY_MESSAGE, substring = true).assertIsDisplayed()
    onNodeWithContentDescription("Play queue").assertDoesNotExist()

    onNodeWithText("The Playing Book").performClick()
    screenEvents.assertEvent(QueueScreenUiEvent.OpenItem(playing))
  }

  @Test
  fun `tapping a queued entry plays it`() = runComposeUiTest {
    setQueue(nowPlaying = playing, queue = listOf(first))

    onNodeWithText("The First Book").performClick()

    assertThat(queueEvents).containsExactly(QueueUiEvent.QueueItemClick(first))
  }

  @Test
  fun `the queue can be cleared once confirmed`() = runComposeUiTest {
    setQueue(nowPlaying = null, queue = listOf(first))

    onNodeWithContentDescription("Clear queue").performClick()
    onNodeWithContentDescription("Confirm clear queue").performClick()

    assertThat(queueEvents).containsExactly(QueueUiEvent.ClearQueue)
  }

  private fun ComposeUiTest.setQueue(nowPlaying: QueuedEntry?, queue: List<QueuedEntry>) {
    setContent {
      CampfireTheme {
        CompositionLocalProvider(
          LocalWindowSizeClass provides WindowSizeClass(minWidthDp = 400, minHeightDp = 800),
          LocalContentLayout provides ContentLayout.Root,
        ) {
          QueueUi(
            state = QueueScreenUiState(
              nowPlaying = nowPlaying,
              queueState = QueueUiState(queue = queue, eventSink = { queueEvents += it }),
              eventSink = screenEvents::invoke,
            ),
          )
        }
      }
    }
  }

  private fun entry(id: String, title: String) = QueuedEntry(
    libraryItem(id = id, media = media(metadata = mediaMetadata(title = title))),
  )

  private companion object {
    const val EMPTY_MESSAGE = "Nothing is queued."
  }
}
