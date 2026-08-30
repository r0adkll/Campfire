// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.preview.libraryItem
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.series.test.FakeSeriesRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SeriesDetailPresenterTest {

  private val screen = SeriesDetailScreen("series_id", "The Stormlight Archive")
  private val navigator = FakeNavigator(screen)
  private val repository = FakeSeriesRepository()
  private val bookInfoRegistry = FakeBookInfoRegistry()
  private val offlineDownloadManager = FakeOfflineDownloadManager()
  private val analytics = FakeAnalytics()

  private val presenter = SeriesDetailPresenter(
    screen = screen,
    navigator = navigator,
    repository = repository,
    bookInfoRegistry = bookInfoRegistry,
    offlineDownloadManager = offlineDownloadManager,
    analytics = analytics,
  )

  private val ownedItem = libraryItem(id = "owned_1")

  private val missingEntry = ProviderSeriesEntry(
    providerBookId = "374131",
    position = 2.0,
    title = "Words of Radiance",
    releaseDate = "2014-03-04",
    isReleased = true,
    providerUrl = "https://hardcover.app/books/words-of-radiance",
    coverUrl = null,
  )

  private val upcomingEntry = ProviderSeriesEntry(
    providerBookId = "999",
    position = 6.0,
    title = "Untitled Stormlight Archive #6",
    releaseDate = "2031-01-01",
    isReleased = false,
    providerUrl = null,
    coverUrl = null,
  )

  private suspend fun emitSeries(
    entries: List<SeriesEntry>,
    providerName: String? = "Hardcover",
  ) {
    repository.seriesLibraryItemsFlow.emit(listOf(ownedItem))
    bookInfoRegistry.seriesEntriesFlow.emit(
      LoadState.Loaded(
        SeriesInfoState(
          providerId = providerName?.let { ProviderId.Hardcover },
          providerName = providerName,
          isCompleted = false,
          entries = entries,
        ),
      ),
    )
  }

  @Test
  fun present_Default_IsLoading() = runTest {
    presenter.test {
      assertThat(awaitItem().seriesContentState).isInstanceOf<LoadState.Loading>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_MergedEntries_AreExposedInOrder() = runTest {
    emitSeries(
      listOf(
        SeriesEntry.Owned(ownedItem),
        SeriesEntry.Missing(missingEntry, ProviderId.Hardcover),
        SeriesEntry.Upcoming(upcomingEntry, ProviderId.Hardcover),
      ),
    )

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      val info = (state.seriesContentState as LoadState.Loaded).data

      assertThat(info.entries.map { it::class.simpleName })
        .isEqualTo(listOf("Owned", "Missing", "Upcoming"))
      assertThat(info.providerName).isEqualTo("Hardcover")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_WithoutProvider_ShowsOnlyOwnedEntries() = runTest {
    emitSeries(listOf(SeriesEntry.Owned(ownedItem)), providerName = null)

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      val info = (state.seriesContentState as LoadState.Loaded).data

      assertThat(info.providerName).isEqualTo(null)
      assertThat(info.entries.single()).isInstanceOf<SeriesEntry.Owned>()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_LibraryItemClick_NavigatesToDetail() = runTest {
    emitSeries(listOf(SeriesEntry.Owned(ownedItem)))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.LibraryItemClick(ownedItem))

      assertThat(navigator.awaitNextScreen()).isEqualTo(
        LibraryItemScreen(
          libraryItemId = ownedItem.id,
          sharedTransitionKey = ownedItem.id + screen.seriesName,
        ),
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_ProviderEntryClick_OpensProviderPage() = runTest {
    emitSeries(listOf(SeriesEntry.Missing(missingEntry, ProviderId.Hardcover)))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.ProviderEntryClick(missingEntry))

      assertThat(navigator.awaitNextScreen())
        .isEqualTo(UrlScreen("https://hardcover.app/books/words-of-radiance"))
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun present_ProviderEntryClickWithoutUrl_DoesNotNavigate() = runTest {
    emitSeries(listOf(SeriesEntry.Upcoming(upcomingEntry, ProviderId.Hardcover)))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.ProviderEntryClick(upcomingEntry))

      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
    navigator.assertGoToIsEmpty()
  }

  @Test
  fun present_Back_PopsNavigator() = runTest {
    emitSeries(listOf(SeriesEntry.Owned(ownedItem)))

    presenter.test {
      val state = awaitItemMatching { it.seriesContentState is LoadState.Loaded<*> }
      state.eventSink(SeriesDetailUiEvent.Back)

      navigator.awaitPop()
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend inline fun app.cash.turbine.ReceiveTurbine<SeriesDetailUiState>.awaitItemMatching(
  predicate: (SeriesDetailUiState) -> Boolean,
): SeriesDetailUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
