// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.core.model.PodcastEpisode
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EpisodeListItemDeleteTest {

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
  private var deletes = 0
  private val queueChanges = mutableListOf<Boolean>()

  @Test
  fun `swiping toward the start past the threshold asks to delete and the row springs back`() = runComposeUiTest {
    setContent { Item(canDelete = true) }
    val restingLeft = onNodeWithText("Episode One").getUnclippedBoundsInRoot().left

    onNodeWithText("Episode One").performTouchInput { swipeLeft(startX = right - 8f, endX = left) }
    waitForIdle()

    assertThat(deletes).isEqualTo(1)
    assertThat(queueChanges).isEmpty()
    assertThat(onNodeWithText("Episode One").getUnclippedBoundsInRoot().left).isEqualTo(restingLeft)
  }

  @Test
  fun `a short swipe toward the start does nothing`() = runComposeUiTest {
    setContent { Item(canDelete = true) }

    onNodeWithText("Episode One").performTouchInput { swipeLeft(startX = right - 8f, endX = right - 60f) }
    waitForIdle()

    assertThat(deletes).isEqualTo(0)
  }

  @Test
  fun `without a delete callback swiping toward the start does nothing`() = runComposeUiTest {
    setContent { Item(canDelete = false) }
    val restingLeft = onNodeWithText("Episode One").getUnclippedBoundsInRoot().left

    onNodeWithText("Episode One").performTouchInput { swipeLeft(startX = right - 8f, endX = left) }
    waitForIdle()

    assertThat(queueChanges).isEmpty()
    assertThat(onNodeWithText("Episode One").getUnclippedBoundsInRoot().left).isEqualTo(restingLeft)
  }

  @Test
  fun `swiping toward the end still queues alongside delete`() = runComposeUiTest {
    setContent { Item(canDelete = true) }

    onNodeWithText("Episode One").performTouchInput { swipeRight(startX = left + 8f, endX = right) }
    waitForIdle()

    assertThat(queueChanges).isEqualTo(listOf(true))
    assertThat(deletes).isEqualTo(0)
  }

  @Test
  fun `delete alone can be swiped toward the start but not the end`() = runComposeUiTest {
    setContent {
      CampfireTheme {
        EpisodeListItem(
          episode = episode,
          onClick = {},
          onPlayClick = {},
          onDelete = { deletes++ },
          modifier = Modifier.width(400.dp),
        )
      }
    }

    onNodeWithText("Episode One").performTouchInput { swipeRight(startX = left + 8f, endX = right) }
    waitForIdle()
    assertThat(deletes).isEqualTo(0)

    onNodeWithText("Episode One").performTouchInput { swipeLeft(startX = right - 8f, endX = left) }
    waitForIdle()
    assertThat(deletes).isEqualTo(1)
  }

  @Test
  fun `delete is offered as an accessibility action for those who can't swipe`() = runComposeUiTest {
    setContent { Item(canDelete = true) }

    val node = onNode(hasCustomAction(DeleteLabel))
    node.fetchSemanticsNode().config[SemanticsActions.CustomActions]
      .first { it.label == DeleteLabel }
      .action()

    assertThat(deletes).isEqualTo(1)
  }

  @Test
  fun `without a delete callback there is no delete action`() = runComposeUiTest {
    setContent { Item(canDelete = false) }

    onNode(hasCustomAction(DeleteLabel)).assertDoesNotExist()
  }

  private fun hasCustomAction(label: String) = SemanticsMatcher("has custom action \"$label\"") { node ->
    node.config.getOrNull(SemanticsActions.CustomActions).orEmpty().any { it.label == label }
  }

  @Composable
  private fun Item(canDelete: Boolean) {
    CampfireTheme {
      EpisodeListItem(
        episode = episode,
        onClick = {},
        onPlayClick = {},
        onQueueChange = { queueChanges += it },
        onDelete = if (canDelete) {
          { deletes++ }
        } else {
          null
        },
        modifier = Modifier.width(400.dp),
      )
    }
  }

  private companion object {
    const val DeleteLabel = "Delete from server…"
  }
}
