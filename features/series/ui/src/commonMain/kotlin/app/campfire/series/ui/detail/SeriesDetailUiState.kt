// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.bookinfo.api.SeriesInfoState
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SeriesDetailUiState(
  val seriesContentState: LoadState<out SeriesInfoState>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  val eventSink: (SeriesDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface SeriesDetailUiEvent : CircuitUiEvent {
  data object Back : SeriesDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : SeriesDetailUiEvent

  /** A book the user doesn't own — opens it on the provider that listed it. */
  data class ProviderEntryClick(val entry: ProviderSeriesEntry) : SeriesDetailUiEvent
}
