// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.common.test.session
import app.campfire.common.test.user
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Media
import app.campfire.core.model.MediaType
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.ui.detail.BaseLibraryItemPresenterTest
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.LibraryItemUiState
import app.campfire.libraries.ui.detail.emptyLibraryItem
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.podcasts.api.screen.FindEpisodesScreen
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.user.test.FakeMediaProgressRepository
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class PodcastPresenterTest : BaseLibraryItemPresenterTest() {

  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "item_id",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )
  private val podcast = emptyLibraryItem().copy(
    mediaType = MediaType.Podcast,
    media = Media.Podcast(
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
    ),
  )
  private val content = DownloadContent.Episode(podcast.id, episode.id)

  // Podcasts open their episodes in an overlay, so the screen needs a host for them
  private val screenPresenter = object : Presenter<LibraryItemUiState> {
    @Composable
    override fun present(): LibraryItemUiState = withCompositionLocal(LocalOverlayHost provides rememberOverlayHost()) {
      presenter.present()
    }
  }

  @Test
  fun episodeDownloads_changeThatEpisodesDownload() = runTest {
    libraryItemRepository.libraryItemFlow.emit(podcast)

    screenPresenter.test {
      val item = awaitLoaded()

      item.eventSink(LibraryItemUiEvent.DownloadEpisodeClick(episode, doNotShowAgain = true))
      item.eventSink(LibraryItemUiEvent.StopEpisodeDownloadClick(episode))
      item.eventSink(LibraryItemUiEvent.RemoveEpisodeDownloadClick(episode))

      assertThat(downloads.invocations).containsExactly(
        FakeDownloads.Invocation.Download(listOf(content)),
        FakeDownloads.Invocation.Stop(content),
        FakeDownloads.Invocation.Delete(content),
      )
      assertThat(libraryViewSettings.showConfirmDownload).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun episodeDownload_needsPermissionToDownload() = runTest {
    userRepository.currentStatefulUserFlow.value = user("user_id", isActive = false)
    libraryItemRepository.libraryItemFlow.emit(podcast)

    screenPresenter.test {
      awaitLoaded().eventSink(LibraryItemUiEvent.DownloadEpisodeClick(episode))

      assertThat(downloads.invocations).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun playAndFinish_actOnTheEpisode() = runTest {
    sessionsRepository.currentSessionFlow.value = session(libraryItem = podcast).copy(episodeId = episode.id)
    libraryItemRepository.libraryItemFlow.emit(podcast)

    screenPresenter.test {
      val item = awaitLoaded()

      item.eventSink(LibraryItemUiEvent.PlayEpisodeClick(episode))
      assertThat((playbackController.session as PlaybackControllerSession.Started).episodeId).isEqualTo("ep_1")

      item.eventSink(LibraryItemUiEvent.MarkEpisodeFinished(episode))
      assertThat(playbackController.session)
        .isEqualTo(PlaybackControllerSession.Stopped(podcast.id, clearQueue = false, episodeId = "ep_1"))
      testScheduler.advanceUntilIdle()
      assertThat(mediaProgressRepository.invocations)
        .contains(FakeMediaProgressRepository.Invocation.MarkFinished(podcast.id, "ep_1"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun findEpisodesAndDownloads_navigate() = runTest {
    libraryItemRepository.libraryItemFlow.emit(podcast)

    screenPresenter.test {
      val item = awaitLoaded()

      item.eventSink(LibraryItemUiEvent.FindEpisodes)
      assertThat(navigator.awaitNextScreen()).isEqualTo(FindEpisodesScreen(podcast.id))

      item.eventSink(LibraryItemUiEvent.OpenDownloads)
      assertThat(navigator.awaitNextScreen()).isEqualTo(PodcastDownloadQueueScreen)
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend fun ReceiveTurbine<LibraryItemUiState>.awaitLoaded(): LibraryItemUiState {
  while (true) {
    val item = awaitItem()
    if (item.contentState is LoadState.Loaded<*>) return item
  }
}
