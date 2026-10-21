// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.author.ui.detail

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.offline.ItemDownloads
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class AuthorDetailUiState(
  val authorContentState: LoadState<out Author>,
  val offlineStates: Map<LibraryItemId, ItemDownloads>,
  val eventSink: (AuthorDetailUiEvent) -> Unit,
) : CircuitUiState

sealed interface AuthorDetailUiEvent : CircuitUiEvent {
  data object Back : AuthorDetailUiEvent
  data class LibraryItemClick(val libraryItem: LibraryItem) : AuthorDetailUiEvent
}
