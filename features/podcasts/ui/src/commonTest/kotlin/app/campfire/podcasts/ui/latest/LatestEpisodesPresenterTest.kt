// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.podcasts.ui.latest

import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.withCompositionLocal
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRecorder
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.model.PodcastEpisode
import app.campfire.libraries.api.ItemChangeNotAllowedException
import app.campfire.offline.Download
import app.campfire.offline.DownloadContent
import app.campfire.offline.test.FakeDownloads
import app.campfire.playlists.api.dialog.AddToPlaylistDialog
import app.campfire.podcasts.api.LatestEpisode
import app.campfire.podcasts.api.RemoteEpisodeDownload
import app.campfire.podcasts.api.screen.LatestEpisodesScreen
import app.campfire.podcasts.api.screen.PodcastDownloadQueueScreen
import app.campfire.podcasts.ui.FakePodcastsRepository
import app.campfire.podcasts.ui.downloads.FakeRemoteEpisodeDownloadTracker
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import app.campfire.user.test.fixtures.user
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
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
import kotlinx.coroutines.test.TestScope
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
  private val userRepository = FakeUserRepository()
  private val podcastsRepository = FakePodcastsRepository()
  private val libraryViewSettings = TestLibraryViewSettings()
  private val workScope = TestScope(UnconfinedTestDispatcher())
  private val downloads = FakeDownloads()
  private val episodeContent = DownloadContent.Episode("podcast", "ep_1")
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
    userRepository = userRepository,
    podcastsRepository = podcastsRepository,
    sessionsRepository = FakeSessionsRepository(),
    mediaProgressRepository = FakeMediaProgressRepository(),
    playbackHistoryRecorder = FakePlaybackHistoryRecorder(),
    playbackController = FakePlaybackController(),
    sessionQueue = sessionQueue,
    downloads = downloads,
    libraryViewSettings = libraryViewSettings,
    addToPlaylistDialog = AddToPlaylistDialog.NoOp,
    remoteEpisodeDownloadTracker = tracker,
    coroutineScopeHolder = CoroutineScopeHolder { workScope },
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
  fun `deleting an episode deletes it from the server and says so`() = runTest {
    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.canDeleteEpisodes).isTrue()

      state.eventSink(LatestEpisodesUiEvent.DeleteEpisode(latest, hardDelete = true))

      awaitSnackbar("Deleted Episode One and its file")
      assertThat(podcastsRepository.deleteEpisodeCalls)
        .containsExactly(FakePodcastsRepository.DeleteEpisodeCall("podcast", "ep_1", hardDelete = true))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `the next delete starts from the last one's choice`() = runTest {
    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.deleteFileByDefault).isFalse()

      state.eventSink(LatestEpisodesUiEvent.DeleteEpisode(latest, hardDelete = true))

      assertThat(libraryViewSettings.deleteEpisodeFilesByDefault).isTrue()
      var latestState = awaitItem()
      while (!latestState.deleteFileByDefault) latestState = awaitItem()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a refused delete says the account isn't allowed`() = runTest {
    podcastsRepository.deleteEpisodeResult = Result.failure(ItemChangeNotAllowedException())

    presenter.test {
      expectMostRecentItem().eventSink(LatestEpisodesUiEvent.DeleteEpisode(latest, hardDelete = false))

      awaitSnackbar("Your account isn't allowed to delete episodes. Ask your server's admin.")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `an account that can't delete isn't offered it`() = runTest {
    val noDelete = user("no_delete").let { it.copy(permissions = it.permissions.copy(delete = false)) }
    userRepository.currentStatefulUserFlow.value = noDelete

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.canDeleteEpisodes).isFalse()

      state.eventSink(LatestEpisodesUiEvent.DeleteEpisode(latest, hardDelete = false))

      assertThat(podcastsRepository.deleteEpisodeCalls).isEmpty()
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

  @Test
  fun `episode downloads show by episode, leaving out books`() = runTest {
    downloads.downloads.value = listOf(
      Download(episodeContent, Download.State.Downloaded),
      Download(DownloadContent.Audiobook("book"), Download.State.Downloaded),
    )

    presenter.test {
      assertThat(expectMostRecentItem().episodeDownloads)
        .isEqualTo(mapOf("ep_1" to Download(episodeContent, Download.State.Downloaded)))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `an episode downloading shows its progress as it goes`() = runTest {
    downloads.downloads.value = listOf(Download(episodeContent, Download.State.Downloading, progress = 0.25f))

    presenter.test {
      assertThat(expectMostRecentItem().episodeDownloads["ep_1"]?.progress).isEqualTo(0.25f)

      downloads.downloads.value = listOf(Download(episodeContent, Download.State.Downloading, progress = 0.75f))
      assertThat(awaitItem().episodeDownloads["ep_1"]?.progress).isEqualTo(0.75f)

      downloads.downloads.value = emptyList()
      assertThat(awaitItem().episodeDownloads).isEmpty()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `downloading an episode starts its download and keeps the confirmation choice`() = runTest {
    libraryViewSettings.showConfirmDownload = true

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.canDownload).isTrue()
      assertThat(state.showConfirmDownloadDialog).isTrue()

      state.eventSink(LatestEpisodesUiEvent.DownloadEpisode(latest, doNotShowAgain = true))

      assertThat(downloads.invocations)
        .containsExactly(FakeDownloads.Invocation.Download(listOf(episodeContent)))
      assertThat(libraryViewSettings.showConfirmDownload).isFalse()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `stopping and removing an episode download act on that episode`() = runTest {
    presenter.test {
      val state = expectMostRecentItem()
      state.eventSink(LatestEpisodesUiEvent.StopDownload(latest))
      state.eventSink(LatestEpisodesUiEvent.RemoveDownload(latest))

      assertThat(downloads.invocations).containsExactly(
        FakeDownloads.Invocation.Stop(episodeContent),
        FakeDownloads.Invocation.Delete(episodeContent),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `a user who may not download can't start one`() = runTest {
    val user = userRepository.currentStatefulUserFlow.value
    userRepository.currentStatefulUserFlow.value = user.copy(
      permissions = user.permissions.copy(download = false),
    )

    presenter.test {
      val state = expectMostRecentItem()
      assertThat(state.canDownload).isFalse()

      state.eventSink(LatestEpisodesUiEvent.DownloadEpisode(latest))

      assertThat(downloads.invocations).isEmpty()
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
