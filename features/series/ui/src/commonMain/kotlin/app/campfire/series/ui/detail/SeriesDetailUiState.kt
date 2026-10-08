// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail

import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.bookinfo.api.SeriesEntry
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class SeriesDetailUiState(
  val seriesContentState: LoadState<out List<LibraryItem>>,
  val offlineStates: Map<LibraryItemId, OfflineDownload>,
  /** Provider-listed books the user doesn't own; null until a provider answers with any. */
  val missingSection: MissingSection? = null,
  /** Owned books [SeriesDetailUiEvent.MarkSeriesFinished] would change. */
  val unfinishedCount: Int = 0,
  /** Owned books [SeriesDetailUiEvent.MarkSeriesNotFinished] would change. */
  val finishedCount: Int = 0,
  /** A whole-series progress change is still working through the books. */
  val isUpdatingProgress: Boolean = false,
  val eventSink: (SeriesDetailUiEvent) -> Unit,
) : CircuitUiState

/** Released series books the user doesn't own. */
data class MissingSection(
  val books: List<SeriesEntry.Missing>,
)

sealed interface SeriesDetailUiEvent : CircuitUiEvent {
  data object Back : SeriesDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : SeriesDetailUiEvent
  data class MissingBookClick(val url: String) : SeriesDetailUiEvent
  data object MarkSeriesFinished : SeriesDetailUiEvent
  data object MarkSeriesNotFinished : SeriesDetailUiEvent
}
