// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast.episode

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.common.compose.layout.ContentLayout
import app.campfire.common.compose.layout.LocalContentLayout
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.ui.detail.SessionUiState
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class PodcastEpisodeBottomSheetTest {

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
  private val events = mutableListOf<PodcastEpisodeUiEvent>()

  private fun state(canDelete: Boolean) = PodcastEpisodeUiState(
    libraryItem = libraryItem(),
    episode = episode,
    progress = null,
    playbackSpeed = 1f,
    isPlaying = false,
    isQueued = false,
    sessionState = SessionUiState.None,
    offlineDownload = null,
    canDownload = true,
    canDelete = canDelete,
    deleteFileByDefault = false,
    showConfirmDownloadDialog = false,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    eventSink = { events += it },
  )

  @Test
  fun deleteButton_asksFirstThenDeletesTheEpisode() = runComposeUiTest {
    setContent { Sheet(state(canDelete = true)) }

    onNodeWithTag("episode_delete").performScrollTo().assertTextEquals("Delete from server…").performClick()

    onNodeWithText("Delete Episode One?").assertIsDisplayed()
    assertThat(events).isEmpty()

    onNodeWithTag("delete_confirm").performClick()

    assertThat(events).containsExactly(PodcastEpisodeUiEvent.DeleteClick(hardDelete = false))
    onNodeWithText("Delete Episode One?").assertDoesNotExist()
  }

  @Test
  fun withoutPermissionToDelete_thereIsNoDeleteButton() = runComposeUiTest {
    setContent { Sheet(state(canDelete = false)) }

    onNodeWithTag("episode_delete").assertDoesNotExist()
  }

  /** The sheet as its overlay shows it, at the root of the content layout. */
  @Composable
  private fun Sheet(state: PodcastEpisodeUiState) {
    CompositionLocalProvider(LocalContentLayout provides ContentLayout.Root) {
      PodcastEpisodeBottomSheet(state)
    }
  }
}
