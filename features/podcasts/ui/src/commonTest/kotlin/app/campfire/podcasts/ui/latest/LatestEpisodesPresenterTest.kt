// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.latest

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.core.model.PodcastEpisode
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.podcasts.ui.FakePodcastsRepository
import app.campfire.podcasts.ui.downloads.FakeRemoteEpisodeDownloadTracker
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.slack.circuit.runtime.presenter.Presenter
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class LatestEpisodesPresenterTest {

  private val sessionQueue = FakeSessionQueue()
  private val navigator = FakeNavigator(LatestEpisodesScreen)
  private val tracker = FakeRemoteEpisodeDownloadTracker()
  private val snackbarHost = SnackbarHostState()
  private val latest = LatestEpisode(
    episode = PodcastEpisode(
      id = "ep_1",
      libraryItemId = "podcast",
      podcastId = "podcast_media",
      title = "Episode One",
      addedAtMillis = 0L,
      updatedAtMillis = 0L,
      durationInMillis = 0L,
      sizeInBytes = 0L,
    ),
    podcastTitle = "The Podcast",
    podcastAuthor = null,
    coverImageUrl = null,
    progress = null,
  )

  private val latestEpisodesPresenter = LatestEpisodesPresenter(
    navigator = navigator,
    userRepository = FakeUserRepository(),
    podcastsRepository = FakePodcastsRepository(),
    sessionsRepository = FakeSessionsRepository(),
    mediaProgressRepository = FakeMediaProgressRepository(),
    playbackHistoryRepository = FakePlaybackHistoryRepository(),
    playbackController = FakePlaybackController(),
    sessionQueue = sessionQueue,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    remoteEpisodeDownloadTracker = tracker,
  )

  // The app's snackbar, where the presenter tells what queueing did
  private val presenter = object : Presenter<LatestEpisodesUiState> {
    @Composable
    override fun present(): LatestEpisodesUiState = withCompositionLocal(LocalSnackBarHost provides snackbarHost) {
      latestEpisodesPresenter.present()
    }
  }

  @BeforeTest
  fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())
  }

  @AfterTest
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `adding to the queue queues the episode and offers undo`() = runTest {
    presenter.test {
      expectMostRecentItem().eventSink(LatestEpisodesUiEvent.AddToQueue(latest))

      assertThat(awaitQueued()).containsOnly("ep_1")
      assertThat(sessionQueue.queue.map { it.key }).containsExactly("podcast:ep_1")

      awaitSnackbar("Added to queue").performAction()

      assertThat(awaitQueued()).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `removing from the queue takes the episode out, and undo puts it back in its place`() = runTest {
    sessionQueue.add("podcast", "ep_1")
    sessionQueue.add("podcast", "ep_2")

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.queuedEpisodeIds).containsOnly("ep_1", "ep_2")

      state.eventSink(LatestEpisodesUiEvent.RemoveFromQueue(latest))

      assertThat(awaitQueued()).containsOnly("ep_2")

      awaitSnackbar("Removed from queue").performAction()

      assertThat(awaitQueued()).containsOnly("ep_1", "ep_2")
      assertThat(sessionQueue.queue.map { it.key }).containsExactly("podcast:ep_1", "podcast:ep_2")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `nothing downloading on the server shows no banner`() = runTest {
    presenter.test {
      assertThat(expectMostRecentItem().serverDownloadCount).isEqualTo(0)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `episodes downloading on the server are counted and open the download queue`() = runTest {
    tracker.setState(
      mapOf(
        "podcast_a" to listOf(download("1", "podcast_a"), download("2", "podcast_a")),
        "podcast_b" to listOf(download("3", "podcast_b")),
      ),
    )

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.serverDownloadCount).isEqualTo(3)

      state.eventSink(LatestEpisodesUiEvent.ServerDownloadsClick)
      assertThat(navigator.awaitNextScreen()).isEqualTo(PodcastDownloadQueueScreen)
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun download(id: String, libraryItemId: String) = RemoteEpisodeDownload(
    id = id,
    libraryItemId = libraryItemId,
    libraryId = null,
    url = "https://example.com/$id.mp3",
    episodeDisplayTitle = null,
    podcastTitle = null,
    state = RemoteEpisodeDownload.State.Queued,
    createdAt = null,
    startedAt = null,
  )

  /** Waits for a state that shows the episodes now in the queue, and returns them. */
  private suspend fun ReceiveTurbine<LatestEpisodesUiState>.awaitQueued(): Set<String> {
    val inQueue = sessionQueue.queue.mapNotNull { it.episodeId }.toSet()
    while (true) {
      val queued = awaitItem().queuedEpisodeIds
      if (queued == inQueue) return queued
    }
  }

  /**
   * Waits for the snackbar to say [message]: its words load off the test's own thread, so this
   * polls from another, and reads nothing there but the snackbar host's state.
   */
  private suspend fun awaitSnackbar(message: String): SnackbarData = withContext(Dispatchers.Default) {
    withTimeout(5.seconds) {
      var shown = snackbarHost.currentSnackbarData
      while (shown?.visuals?.message != message) {
        delay(10)
        shown = snackbarHost.currentSnackbarData
      }
      shown
    }
  }
}
