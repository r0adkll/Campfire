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
import app.campfire.analytics.events.ContentSelected
import app.campfire.analytics.events.ContentType
import app.campfire.audioplayer.offline.OfflineDownloadManager
import app.campfire.bookinfo.api.BookInfoProviderSettings
import app.campfire.bookinfo.api.BookInfoRegistry
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.common.screens.SeriesDetailScreen
import app.campfire.common.screens.UrlScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.loggableId
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.series.api.SeriesRepository
import com.r0adkll.kimchi.circuit.annotations.CircuitInject
import com.slack.circuit.foundation.NonPausablePresenter
import com.slack.circuit.runtime.Navigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.tatarka.inject.annotations.Assisted
import me.tatarka.inject.annotations.Inject

@CircuitInject(SeriesDetailScreen::class, UserScope::class)
@Inject
class SeriesDetailPresenter(
  @Assisted private val screen: SeriesDetailScreen,
  @Assisted private val navigator: Navigator,
  private val repository: SeriesRepository,
  private val offlineDownloadManager: OfflineDownloadManager,
  private val bookInfoRegistry: BookInfoRegistry,
  private val bookInfoSettings: BookInfoProviderSettings,
  private val analytics: Analytics,
) : NonPausablePresenter<SeriesDetailUiState> {

  @Suppress("UNCHECKED_CAST")
  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  override fun present(): SeriesDetailUiState {
    val seriesContentState by remember {
      repository.observeSeriesLibraryItems(seriesId = screen.seriesId)
        .map { LoadState.Loaded(it) as LoadState<List<LibraryItem>> }
        .catch { emit(LoadState.Error as LoadState<List<LibraryItem>>) }
    }.collectAsState(LoadState.Loading)

    LaunchedEffect(seriesContentState) {
      val groupedBooks = seriesContentState.dataOrNull
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
        .flatMapLatest { items ->
          offlineDownloadManager.observeForItems(items)
        }
    }.collectAsState(emptyMap())

    // Checks the provider automatically — served from the registry's series
    // cache, so this is usually a local read. The section only exists once a
    // provider answers with books the user doesn't own, and the whole lookup
    // is skipped (not just hidden) when the user turns the feature off.
    val missingSection by remember {
      bookInfoSettings.observeSeriesMissingBooksEnabled()
        .distinctUntilChanged()
        .flatMapLatest { showMissing ->
          if (!showMissing) return@flatMapLatest flowOf(null)
          snapshotFlow { seriesContentState.dataOrNull }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { items ->
              bookInfoRegistry.observeSeriesEntries(screen.seriesName, items)
            }
            .map { loadState ->
              val state = loadState.dataOrNull ?: return@map null
              // A null provider means owned-only data — the provider hasn't answered.
              if (state.providerId == null) return@map null
              val missing = state.entries.filterIsInstance<SeriesEntry.Missing>()
              if (missing.isEmpty()) null else MissingSection(missing)
            }
            .catch { emit(null) }
        }
    }.collectAsState(null)

    return SeriesDetailUiState(
      seriesContentState = seriesContentState,
      offlineStates = offlineDownloads,
      missingSection = missingSection,
    ) { event ->
      when (event) {
        SeriesDetailUiEvent.Back -> navigator.pop()
        is SeriesDetailUiEvent.MissingBookClick -> navigator.goTo(UrlScreen(event.url))
        is SeriesDetailUiEvent.LibraryItemClick -> {
          analytics.send(ContentSelected(ContentType.LibraryItem))
          navigator.goTo(
            LibraryItemScreen(
              libraryItemId = event.libraryItem.id,
              sharedTransitionKey = event.libraryItem.id + screen.seriesName,
            ),
          )
        }
      }
    }
  }
}
