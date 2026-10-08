// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.FakePlaybackController
import app.campfire.audioplayer.test.PlaybackControllerSession
import app.campfire.audioplayer.test.history.FakePlaybackHistoryRepository
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoProviderSettings
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.test.mediaProgress
import app.campfire.common.test.session
import app.campfire.core.coroutines.CoroutineScopeHolder
import app.campfire.core.coroutines.LoadState
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.series.test.FakeSeriesRepository
import app.campfire.sessions.test.FakeSessionsRepository
import app.campfire.user.test.FakeMediaProgressRepository
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

class SeriesDetailPresenterTest {

  private val screen = SeriesDetailScreen("s1", "The Stormlight Archive")
  private val navigator = FakeNavigator(screen)
  private val repository = FakeSeriesRepository()
  private val registry = FakeBookInfoRegistry()
  private val settings = FakeBookInfoProviderSettings()
  private val mediaProgressRepository = FakeMediaProgressRepository()
  private val sessionsRepository = FakeSessionsRepository()
  private val playbackController = FakePlaybackController()

  private fun presenter(userScope: CoroutineScope) = SeriesDetailPresenter(
    screen = screen,
    navigator = navigator,
    repository = repository,
    offlineDownloadManager = FakeOfflineDownloadManager(),
    bookInfoRegistry = registry,
    bookInfoSettings = settings,
    mediaProgressRepository = mediaProgressRepository,
    sessionsRepository = sessionsRepository,
    playbackController = playbackController,
    playbackHistoryRepository = FakePlaybackHistoryRepository(),
    userScopeHolder = CoroutineScopeHolder { userScope },
    analytics = FakeAnalytics(),
  )

  private val TestScope.presenter get() = presenter(backgroundScope)

  private val owned = libraryItem(
    media = media(metadata = mediaMetadata(title = "The Way of Kings", ASIN = "B003P2WO5E")),
  )

  private val finished = libraryItem(
    id = "finished",
    userMediaProgress = mediaProgress(libraryItemId = "finished", isFinished = true),
  )
  private val inProgress = libraryItem(
    id = "in-progress",
    userMediaProgress = mediaProgress(libraryItemId = "in-progress", progress = 0.5f),
  )
  private val unstarted = libraryItem(id = "unstarted")

  private val missingEntry = ProviderSeriesEntry(
    providerBookId = "B00BWWSVPU",
    position = 2.0,
    title = "Words of Radiance",
    releaseDate = "2014-03-04",
    isReleased = true,
    providerUrl = "https://www.audible.com/pd/B00BWWSVPU",
    coverUrl = null,
  )

  @Test
  fun present_MissingBooks_SurfaceWhenEnabled() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(owned))
    registry.seriesEntriesFlow.emit(
      LoadState.Loaded(
        SeriesInfoState(
          providerId = ProviderId.Audible,
          providerName = "Audible",
          isCompleted = null,
          entries = listOf(
            SeriesEntry.Owned(owned),
            SeriesEntry.Missing(missingEntry, ProviderId.Audible),
          ),
        ),
      ),
    )

    presenter.test {
      val state = awaitItemMatching { it.missingSection != null }

      assertThat(state.missingSection!!.books.map { it.entry.title })
        .isEqualTo(listOf("Words of Radiance"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_MissingBooks_NeverLoadWhenDisabled() = runTest {
    settings.seriesMissingBooks.value = false
    repository.seriesLibraryItemsFlow.emit(listOf(owned))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }

      // Disabled means skipped entirely — no registry subscription, no section.
      assertThat(state.missingSection).isNull()
      assertThat(registry.seriesEntriesRequests.size).isEqualTo(0)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_TogglingOff_RemovesTheSection() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(owned))
    registry.seriesEntriesFlow.emit(
      LoadState.Loaded(
        SeriesInfoState(
          providerId = ProviderId.Audible,
          providerName = "Audible",
          isCompleted = null,
          entries = listOf(SeriesEntry.Missing(missingEntry, ProviderId.Audible)),
        ),
      ),
    )

    presenter.test {
      awaitItemMatching { it.missingSection != null }

      settings.seriesMissingBooks.value = false

      awaitItemMatching { it.missingSection == null }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_ProgressCounts_SplitFinishedFromTheRest() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(finished, inProgress, unstarted))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }

      assertThat(state.finishedCount).isEqualTo(1)
      assertThat(state.unfinishedCount).isEqualTo(2)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markSeriesFinished_MarksOnlyUnfinishedBooks_AndStopsThePlayingOne() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(finished, inProgress, unstarted))
    sessionsRepository.currentSession = session(libraryItem = inProgress)

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.MarkSeriesFinished)
      awaitItemMatching { it.isUpdatingProgress }
      awaitItemMatching { !it.isUpdatingProgress }

      assertThat(markedFinishedIds()).containsExactly("in-progress", "unstarted")
      assertThat(markedDeletedSessionIds()).containsExactly("in-progress", "unstarted")
      assertThat(playbackController.session)
        .isEqualTo(PlaybackControllerSession.Stopped(itemId = "in-progress", clearQueue = false))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markSeriesFinished_LeavesPlaybackOfOtherBooksAlone() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(unstarted))
    sessionsRepository.currentSession = session(libraryItem = libraryItem(id = "other-series"))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.MarkSeriesFinished)
      awaitItemMatching { it.isUpdatingProgress }
      awaitItemMatching { !it.isUpdatingProgress }

      assertThat(markedFinishedIds()).containsExactly("unstarted")
      assertThat(playbackController.session).isEqualTo(PlaybackControllerSession.None)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun markSeriesNotFinished_ResetsOnlyFinishedBooks() = runTest {
    repository.seriesLibraryItemsFlow.emit(listOf(finished, inProgress, unstarted))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.MarkSeriesNotFinished)
      awaitItemMatching { it.isUpdatingProgress }
      awaitItemMatching { !it.isUpdatingProgress }

      assertThat(
        mediaProgressRepository.invocations
          .filterIsInstance<FakeMediaProgressRepository.Invocation.MarkNotFinished>()
          .map { it.libraryItemId },
      ).containsExactly("finished")
      assertThat(markedFinishedIds()).isEqualTo(emptyList())
      cancelAndIgnoreRemainingEvents()
    }
  }

  private fun markedFinishedIds() = mediaProgressRepository.invocations
    .filterIsInstance<FakeMediaProgressRepository.Invocation.MarkFinished>()
    .map { it.libraryItemId }

  private fun markedDeletedSessionIds() = sessionsRepository.invocations
    .filterIsInstance<FakeSessionsRepository.Invocation.MarkDeleted>()
    .map { it.libraryItemId }
}

private suspend inline fun app.cash.turbine.ReceiveTurbine<SeriesDetailUiState>.awaitItemMatching(
  predicate: (SeriesDetailUiState) -> Boolean,
): SeriesDetailUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
