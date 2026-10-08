// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.Playlist
import app.campfire.home.ui.libraryItem
import app.campfire.playlists.api.screen.PlaylistDetailScreen
import app.campfire.playlists.ui.list.FakePlaylistsRepository
import app.campfire.sessions.test.FakeSessionQueue
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.settings.test.TestLibraryViewSettings
import app.campfire.user.api.MediaProgressKey
import app.campfire.user.test.FakeMediaProgressRepository
import app.campfire.user.test.FakeUserRepository
import app.cash.turbine.ReceiveTurbine
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.time.Duration
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime

class PlaylistDetailPresenterTest {

  private val screen = PlaylistDetailScreen("playlist", "Bedtime", null)
  private val playlistsRepository = FakePlaylistsRepository()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val sessionsRepository = FakeSessionsRepository()
  private val playbackController = FakePlaybackController()
  private val playbackHistoryRepository = FakePlaybackHistoryRepository()

  private val presenter = PlaylistDetailPresenter(
    screen = screen,
    navigator = FakeNavigator(screen),
    analytics = FakeAnalytics(),
    playlistsRepository = playlistsRepository,
    playbackController = playbackController,
    sessionsRepository = sessionsRepository,
    sessionQueue = FakeSessionQueue(),
    downloadManager = FakeOfflineDownloadManager(),
    userRepository = FakeUserRepository(),
    libraryViewSettings = TestLibraryViewSettings(),
    mediaProgressRepository = mediaProgressRepository,
    playbackHistoryRepository = playbackHistoryRepository,
  )

  @Test
  fun markAllFinished_finishesOnlyTheUnfinishedItems() = runTest {
    val playing = libraryItem(id = "playing")
    playlistsRepository.playlistItemsFlow.value = listOf(
      item("finished", index = 0),
      item(playing.id, index = 1),
      item("untouched", index = 2),
      // The same podcast twice, once per episode, has a progress row for each
      item("podcast", index = 3, episodeId = "episode-1"),
      item("podcast", index = 4, episodeId = "episode-2"),
    )
    mediaProgressRepository.allProgressFlow.value = listOf(
      mediaProgress("finished", isFinished = true),
      mediaProgress(playing.id, currentTime = 50f),
    )
    sessionsRepository.currentSessionFlow.value = session(libraryItem = playing)
    playbackHistoryRepository.history[FakePlaybackHistoryRepository.HistoryKey(playing.id)] =
      listOf(playAction(playing.id))

    presenter.test {
      val state = awaitState { it.playlistItems.size == 5 && it.progressStates.size == 2 }
      assertThat(state.canMarkAllFinished).isTrue()

      state.eventSink(PlaylistDetailUiEvent.MarkAllFinished)

      val finishing = listOf(
        MediaProgressKey(playing.id),
        MediaProgressKey("untouched"),
        MediaProgressKey("podcast", "episode-1"),
        MediaProgressKey("podcast", "episode-2"),
      )
      assertThat(mediaProgressRepository.invocations.filterIsInstance<MarkAllFinished>())
        .containsExactly(MarkAllFinished(finishing))
      assertThat(sessionsRepository.invocations.filterIsInstance<MarkDeleted>())
        .containsExactly(*finishing.map { MarkDeleted(it.libraryItemId, it.episodeId) }.toTypedArray())
      // The current session is one being finished, so playback stops
      assertThat(playbackController.session)
        .isEqualTo(PlaybackControllerSession.Stopped(itemId = playing.id, clearQueue = false))
      assertThat(playbackHistoryRepository.history.getValue(FakePlaybackHistoryRepository.HistoryKey(playing.id)))
        .isEmpty()

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markAllFinished_leavesPlaybackAloneWhenTheCurrentItemIsNotAffected() = runTest {
    playlistsRepository.playlistItemsFlow.value = listOf(item("unfinished", index = 0))
    sessionsRepository.currentSessionFlow.value = session(libraryItem = libraryItem(id = "elsewhere"))

    presenter.test {
      val state = awaitState { it.playlistItems.size == 1 && it.currentSession != null }

      state.eventSink(PlaylistDetailUiEvent.MarkAllFinished)

      assertThat(mediaProgressRepository.invocations.filterIsInstance<MarkAllFinished>())
        .containsExactly(MarkAllFinished(listOf(MediaProgressKey("unfinished"))))
      assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markAllNotFinished_unfinishesOnlyTheFinishedItems() = runTest {
    playlistsRepository.playlistItemsFlow.value = listOf(
      item("finished", index = 0),
      item("in-progress", index = 1),
      item("untouched", index = 2),
    )
    mediaProgressRepository.allProgressFlow.value = listOf(
      mediaProgress("finished", isFinished = true),
      mediaProgress("in-progress", currentTime = 50f),
    )

    presenter.test {
      val state = awaitState { it.playlistItems.size == 3 && it.progressStates.size == 2 }
      assertThat(state.canMarkAllNotFinished).isTrue()

      state.eventSink(PlaylistDetailUiEvent.MarkAllNotFinished)

      assertThat(mediaProgressRepository.invocations.filterIsInstance<MarkAllNotFinished>())
        .containsOnly(MarkAllNotFinished(listOf(MediaProgressKey("finished"))))

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markAll_isOnlyOfferedWhenItWouldChangeSomething() = runTest {
    playlistsRepository.playlistItemsFlow.value = listOf(item("a", index = 0), item("b", index = 1))
    mediaProgressRepository.allProgressFlow.value = listOf(
      mediaProgress("a", isFinished = true),
      mediaProgress("b", isFinished = true),
    )

    presenter.test {
      val allFinished = awaitState { it.playlistItems.size == 2 && it.progressStates.size == 2 }
      assertThat(allFinished.canMarkAllFinished).isFalse()
      assertThat(allFinished.canMarkAllNotFinished).isTrue()

      // Nothing left to finish, so the event is a no-op
      allFinished.eventSink(PlaylistDetailUiEvent.MarkAllFinished)
      assertThat(mediaProgressRepository.invocations.filterIsInstance<MarkAllFinished>()).isEmpty()

      mediaProgressRepository.allProgressFlow.value = emptyList()
      val noneFinished = awaitState { it.progressStates.isEmpty() }
      assertThat(noneFinished.canMarkAllFinished).isTrue()
      assertThat(noneFinished.canMarkAllNotFinished).isFalse()

      cancelAndIgnoreRemainingEvents()
    }
  }

  private suspend fun ReceiveTurbine<PlaylistDetailUiState>.awaitState(
    predicate: (PlaylistDetailUiState) -> Boolean,
  ): PlaylistDetailUiState {
    var state = awaitItem()
    while (!predicate(state)) state = awaitItem()
    return state
  }

  private fun item(
    libraryItemId: String,
    index: Int,
    episodeId: String? = null,
  ) = Playlist.Item.Expanded(
    index = index,
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    libraryItem = libraryItem(id = libraryItemId),
  )

  private fun playAction(libraryItemId: String) = PlaybackAction(
    id = 1L,
    libraryItemId = libraryItemId,
    userId = "user",
    type = PlaybackActionType.Play,
    timestamp = LocalDateTime(2026, 1, 1, 0, 0),
    fromPosition = Duration.ZERO,
    toPosition = null,
  )
}

private typealias MarkAllFinished = FakeMediaProgressRepository.Invocation.MarkAllFinished
private typealias MarkAllNotFinished = FakeMediaProgressRepository.Invocation.MarkAllNotFinished
private typealias MarkDeleted = FakeSessionsRepository.Invocation.MarkDeleted
