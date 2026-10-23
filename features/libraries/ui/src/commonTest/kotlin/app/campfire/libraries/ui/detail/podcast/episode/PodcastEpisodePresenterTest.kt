// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.podcast.episode

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.FakeAudioPlayerHolder
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.common.test.user
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class PodcastEpisodePresenterTest {

  private val item = libraryItem(id = "podcast")
  private val episode = PodcastEpisode(
    id = "ep_1",
    libraryItemId = "podcast",
    podcastId = "podcast_media",
    title = "Episode One",
    addedAtMillis = 0L,
    updatedAtMillis = 0L,
    durationInMillis = 0L,
    sizeInBytes = 0L,
  )
  private val content = DownloadContent.Episode("podcast", "ep_1")

  private val downloads = FakeDownloads()
  private val userRepository = FakeUserRepository()
  private val libraryViewSettings = TestLibraryViewSettings()

  private val presenter = PodcastEpisodePresenter(
    libraryItem = item,
    episode = episode,
    navigator = object : OverlayNavigator<Unit> {
      override fun finish(result: Unit) = Unit
    },
    analytics = FakeAnalytics(),
    playbackController = FakePlaybackController(),
    sessionsRepository = FakeSessionsRepository(),
    sessionQueue = FakeSessionQueue(),
    mediaProgressRepository = FakeMediaProgressRepository(),
    playbackHistoryRepository = FakePlaybackHistoryRepository(),
    audioPlayerHolder = FakeAudioPlayerHolder(),
    downloads = downloads,
    userRepository = userRepository,
    libraryViewSettings = libraryViewSettings,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
  )

  @Test
  fun present_showsThisEpisodesDownload() = runTest {
    val thisEpisode = Download(content, Download.State.Downloading, progress = 0.25f)
    downloads.downloads.value = listOf(
      Download(DownloadContent.Episode("podcast", "ep_2"), Download.State.Downloaded),
      thisEpisode,
    )

    presenter.test {
      assertThat(awaitItemMatching { it.offlineDownload != null }.offlineDownload).isEqualTo(thisEpisode)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun downloadClick_downloadsThisEpisode_andKeepsTheConfirmChoice() = runTest {
    presenter.test {
      awaitItem().eventSink(PodcastEpisodeUiEvent.DownloadClick(doNotShowAgain = true))

      assertThat(downloads.invocations).containsExactly(FakeDownloads.Invocation.Download(listOf(content)))
      assertThat(libraryViewSettings.showConfirmDownload).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun downloadClick_withoutPermissionToDownload_doesNothing() = runTest {
    userRepository.currentStatefulUserFlow.value = user("user_id", isActive = false)

    presenter.test {
      awaitItemMatching { !it.canDownload }.eventSink(PodcastEpisodeUiEvent.DownloadClick())

      assertThat(downloads.invocations).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun stopAndRemove_changeThisEpisodesDownload() = runTest {
    presenter.test {
      val eventSink = awaitItem().eventSink
      eventSink(PodcastEpisodeUiEvent.StopDownloadClick)
      eventSink(PodcastEpisodeUiEvent.RemoveDownloadClick)

      assertThat(downloads.invocations).containsExactly(
        FakeDownloads.Invocation.Stop(content),
        FakeDownloads.Invocation.Delete(content),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend inline fun ReceiveTurbine<PodcastEpisodeUiState>.awaitItemMatching(
  predicate: (PodcastEpisodeUiState) -> Boolean,
): PodcastEpisodeUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
