// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.PodcastEpisode
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EpisodeListItemQueueTest {

  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 60_000L,
    sizeInBytes = 0L,
  )
  private val queueChanges = mutableListOf<Boolean>()
  private var clicked = false

  @Test
  fun `swiping past the threshold queues the episode and the row springs back`() = runComposeUiTest {
    setContent { Item(isQueued = false) }
    val restingLeft = onNodeWithText("Episode One").getUnclippedBoundsInRoot().left

    onNodeWithText("Episode One").performTouchInput { swipeRight(startX = left + 8f, endX = right) }
    waitForIdle()

    assertThat(queueChanges).containsExactly(true)
    assertThat(clicked).isFalse()
    assertThat(onNodeWithText("Episode One").getUnclippedBoundsInRoot().left).isEqualTo(restingLeft)
  }

  @Test
  fun `a short swipe does nothing`() = runComposeUiTest {
    setContent { Item(isQueued = false) }

    onNodeWithText("Episode One").performTouchInput { swipeRight(startX = left + 8f, endX = left + 60f) }
    waitForIdle()

    assertThat(queueChanges).isEmpty()
  }

  @Test
  fun `swiping a queued episode takes it out of the queue`() = runComposeUiTest {
    setContent { Item(isQueued = true) }

    onNodeWithText("Episode One").performTouchInput { swipeRight(startX = left + 8f, endX = right) }
    waitForIdle()

    assertThat(queueChanges).containsExactly(false)
  }

  @Test
  fun `the queue button adds the episode, then takes it out`() = runComposeUiTest {
    var isQueued by mutableStateOf(false)
    setContent { Item(isQueued = isQueued) }

    onNodeWithContentDescription("Add to queue").performClick()
    isQueued = true
    onNodeWithContentDescription("Remove from queue").performClick()

    assertThat(queueChanges).containsExactly(true, false)
  }

  @Test
  fun `without a queue callback there is no queue button`() = runComposeUiTest {
    setContent {
      CampfireTheme {
        EpisodeListItem(episode = episode, onClick = {}, onPlayClick = {})
      }
    }

    onNodeWithContentDescription("Add to queue").assertDoesNotExist()
  }

  @Composable
  private fun Item(isQueued: Boolean) {
    CampfireTheme {
      EpisodeListItem(
        episode = episode,
        onClick = { clicked = true },
        onPlayClick = {},
        isQueued = isQueued,
        onQueueChange = { queueChanges += it },
        modifier = Modifier.width(400.dp),
      )
    }
  }
}
