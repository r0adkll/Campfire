// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.FileMetadata
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.home.ui.libraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import assertk.assertThat
import assertk.assertions.containsExactly
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class, ExperimentalSharedTransitionApi::class)
class EpisodeSlotTest {

  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 60_000L,
    sizeInBytes = 0L,
    audioTrack = AudioTrack(
      index = 1,
      startOffset = 0f,
      duration = 60f,
      title = "episode.mp3",
      contentUrl = "/api/items/podcast/file/1",
      mimeType = "audio/mpeg",
      codec = "mp3",
      metadata = FileMetadata(
        filename = "episode.mp3",
        ext = "mp3",
        path = "/podcasts/episode.mp3",
        relPath = "episode.mp3",
        size = 0L,
        mtimeMs = 0L,
        ctimeMs = 0L,
        birthtimeMs = 0L,
      ),
      metaTags = null,
    ),
  )
  private val media = Media.Podcast(
    id = "podcast_media",
    metadata = Media.Metadata.Podcast(
      title = "The Podcast",
      author = "Author",
      description = null,
      releaseDate = null,
      genres = emptyList(),
      feedUrl = null,
      imageUrl = null,
      itunesPageUrl = null,
      itunesId = null,
      itunesArtistId = null,
      isExplicit = false,
      language = null,
      podcastType = null,
    ),
    coverImageUrl = "",
    coverPath = null,
    tags = emptyList(),
    sizeInBytes = 0L,
    episodes = listOf(episode),
  )
  private val events = mutableListOf<LibraryItemUiEvent>()

  private fun slot(download: Download.State?, canDownload: Boolean = true) = EpisodeSlot(
    libraryItem = libraryItem(id = "podcast").copy(media = media),
    media = media,
    episode = episode,
    progress = null,
    isCurrentSession = false,
    offlineDownload = download?.let { Download(DownloadContent.Episode("podcast", "ep_1"), it) },
    canDownload = canDownload,
    showConfirmDownloadDialog = false,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
  )

  @Test
  fun notDownloaded_downloadsTheEpisode() = runComposeUiTest {
    val slot = slot(download = null)
    setContent { PreviewSharedElementTransitionLayout { slot.Content(Modifier) { events += it } } }

    onNodeWithContentDescription("Download episode for offline").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.DownloadEpisodeClick(episode))
  }

  @Test
  fun failedDownload_downloadsAgain() = runComposeUiTest {
    val slot = slot(download = Download.State.Failed)
    setContent { PreviewSharedElementTransitionLayout { slot.Content(Modifier) { events += it } } }

    onNodeWithContentDescription("Download episode for offline").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.DownloadEpisodeClick(episode))
  }

  @Test
  fun downloaded_removesTheDownload() = runComposeUiTest {
    val slot = slot(download = Download.State.Downloaded)
    setContent { PreviewSharedElementTransitionLayout { slot.Content(Modifier) { events += it } } }

    onNodeWithContentDescription("Remove offline episode").performClick()

    assertThat(events).containsExactly(LibraryItemUiEvent.RemoveEpisodeDownloadClick(episode))
  }

  @Test
  fun withoutPermissionToDownload_aDownloadStaysRemovable() = runComposeUiTest {
    val slot = slot(download = Download.State.Downloaded, canDownload = false)
    setContent { PreviewSharedElementTransitionLayout { slot.Content(Modifier) { events += it } } }

    onNodeWithContentDescription("Remove offline episode").assertIsDisplayed()
  }

  @Test
  fun withoutPermissionToDownload_offersNoDownload() = runComposeUiTest {
    val slot = slot(download = null, canDownload = false)
    setContent { PreviewSharedElementTransitionLayout { slot.Content(Modifier) { events += it } } }

    onNodeWithContentDescription("Download episode for offline").assertDoesNotExist()
  }
}
