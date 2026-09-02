// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import app.campfire.analytics.test.FakeAnalytics
import app.campfire.audioplayer.test.offline.FakeOfflineDownloadManager
import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.bookinfo.test.FakeBookInfoProviderSettings
import app.campfire.bookinfo.test.FakeBookInfoRegistry
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.home.ui.libraryItem
import app.campfire.home.ui.media
import app.campfire.home.ui.mediaMetadata
import app.campfire.series.test.FakeSeriesRepository
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class SeriesDetailPresenterTest {

  private val screen = SeriesDetailScreen("s1", "The Stormlight Archive")
  private val navigator = FakeNavigator(screen)
  private val repository = FakeSeriesRepository()
  private val registry = FakeBookInfoRegistry()
  private val settings = FakeBookInfoProviderSettings()

  private val presenter = SeriesDetailPresenter(
    screen = screen,
    navigator = navigator,
    repository = repository,
    offlineDownloadManager = FakeOfflineDownloadManager(),
    bookInfoRegistry = registry,
    bookInfoSettings = settings,
    analytics = FakeAnalytics(),
  )

  private val owned = libraryItem(
    media = media(metadata = mediaMetadata(title = "The Way of Kings", ASIN = "B003P2WO5E")),
  )

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
}

private suspend inline fun app.cash.turbine.ReceiveTurbine<SeriesDetailUiState>.awaitItemMatching(
  predicate: (SeriesDetailUiState) -> Boolean,
): SeriesDetailUiState {
  while (true) {
    val item = awaitItem()
    if (predicate(item)) return item
  }
}
