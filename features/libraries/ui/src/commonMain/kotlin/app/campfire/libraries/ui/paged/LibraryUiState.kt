// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.paged

import androidx.compose.runtime.Immutable
import androidx.paging.compose.LazyPagingItems
import app.campfire.core.filter.ContentFilter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection
import app.campfire.core.settings.SortModeConfig
import app.campfire.offline.ItemDownloads
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryUiState(
  val isPodcastLibrary: Boolean,
  val canAddPodcasts: Boolean,
  val lazyPagingItems: LazyPagingItems<LibraryItem>,
  val itemDisplayState: ItemDisplayState,
  val totalItemCount: Int,
  val sort: LibrarySort,
  val filter: ContentFilter?,
  val offlineStates: Map<LibraryItemId, ItemDownloads>,
  /** Each item's progress as it is now, or null until it's known and the pages' own stand in */
  val progress: Map<LibraryItemId, MediaProgress>?,
  // Podcasts only: how many of each show's episodes the user hasn't finished
  val unfinishedEpisodes: Map<LibraryItemId, Int>,
  // Podcasts only: episodes the server is downloading, shown as a banner while there are any
  val serverDownloadCount: Int = 0,
  val eventSink: (LibraryUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class LibrarySort(
  val mode: ContentSortMode,
  val direction: SortDirection,
  val config: SortModeConfig,
)

sealed interface LibraryUiEvent : CircuitUiEvent {
  data object ToggleItemDisplayState : LibraryUiEvent
  data class SortModeSelected(val mode: ContentSortMode) : LibraryUiEvent
  data class ItemFilterSelected(val filter: ContentFilter?) : LibraryUiEvent
  data class ItemClick(val libraryItem: LibraryItem) : LibraryUiEvent
  data object AddPodcastClick : LibraryUiEvent
  data object ServerDownloadsClick : LibraryUiEvent
}

/** [item]'s progress as it is now, or as its page has it while that isn't known */
internal fun Map<LibraryItemId, MediaProgress>?.progressOf(item: LibraryItem): MediaProgress? =
  if (this != null) this[item.id] else item.userMediaProgress
