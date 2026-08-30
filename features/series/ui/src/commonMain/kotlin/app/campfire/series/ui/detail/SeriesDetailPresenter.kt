// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.ActionEvent
import app.campfire.analytics.events.Click
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.loggableId
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Inject
class SeriesDetailPresenter(
  @Assisted private val screen: SeriesDetailScreen,
  @Assisted private val navigator: Navigator,
  private val repository: SeriesRepository,
  private val bookInfoRegistry: BookInfoRegistry,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val analytics: Analytics,
) : NonPausablePresenter<SeriesDetailUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SeriesDetailUiState {
    val seriesContentState by remember {
      repository.observeSeriesLibraryItems(seriesId = screen.seriesId)
        .flatMapLatest { items ->
          bookInfoRegistry.observeSeriesEntries(screen.seriesName, items)
        }
        .catch { emit(LoadState.Error as LoadState<SeriesInfoState>) }
    }.collectAsState(LoadState.Loading)

    LaunchedEffect(seriesContentState) {
      val groupedBooks = seriesContentState.dataOrNull
        ?.entries
        ?.filterIsInstance<SeriesEntry.Owned>()
        ?.map { it.item }
        ?.groupBy { item -> item.id }
        ?: emptyMap()

      groupedBooks.forEach { (key, value) ->
        if (value.size > 1) {
          bark { "Series Item Overlap! [${key.loggableId}]" }
          value.forEach {
            // Log a hash instead of the item itself — LibraryItem's toString carries
            // cover/track URLs — while still showing whether the duplicates differ.
            bark { "-- ${it.id.loggableId} hash=${it.hashCode()}" }
          }
        }
      }
    }

    val offlineDownloads by remember {
      snapshotFlow { seriesContentState.dataOrNull }
        .filterNotNull()
        .flatMapLatest { info ->
          offlineDownloadManager.observeForItems(
            info.entries.filterIsInstance<SeriesEntry.Owned>().map { it.item },
          )
        }
    }.collectAsState(emptyMap())

    return SeriesDetailUiState(
      seriesContentState = seriesContentState,
      offlineStates = offlineDownloads,
    ) { event ->
      when (event) {
        SeriesDetailUiEvent.Back -> navigator.pop()
        is SeriesDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.libraryItem.id,
              sharedTransitionKey = event.libraryItem.id + screen.seriesName,
            ),
          )
        }

        is SeriesDetailUiEvent.ProviderEntryClick -> {
          analytics.send(ActionEvent("series_provider_entry", Click))
          event.entry.providerUrl?.let { navigator.goTo(UrlScreen(it)) }
        }
      }
    }
  }
}
