// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.editor

import androidx.compose.runtime.Stable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.LibraryItem
import app.campfire.management.ui.match.MatchUiState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class ItemEditorUiState(
  /** The item as the server has it */
  val item: LoadState<out LibraryItem>,
  /** The details being edited, null until the item has loaded */
  val draft: BookDetailsDraft?,
  val section: EditorSection,
  val match: MatchUiState,
  /** The source the form was last filled in from, to say so until it's saved */
  val filledFrom: String?,
  /** Whether the account may change items, as the server would check */
  val canEdit: Boolean,
  /** Whether the account may see explicit items, which marking this one explicit would hide */
  val canSeeExplicit: Boolean,
  /** Names the book's library already has, to suggest while typing */
  val suggestions: DetailSuggestions,
  val isSaving: Boolean,
  /** The server's details changed since editing started, and the draft hasn't caught up */
  val isBehindServer: Boolean,
  /** Closing with changes waits on whether to throw them away */
  val isConfirmingDiscard: Boolean,
  /** The server's copy of the details couldn't be fetched, so there's nothing to edit */
  val isLoadFailed: Boolean,
  val eventSink: (ItemEditorUiEvent) -> Unit,
) : CircuitUiState

/** A part of the editor, each its own tab */
enum class EditorSection {
  Details,

  /** Finding the book in a source, like Audible, to fill in its details from */
  Match,
}

sealed interface ItemEditorUiEvent : CircuitUiEvent {
  data object Save : ItemEditorUiEvent

  /** Leave, asking first when there are changes */
  data object Close : ItemEditorUiEvent

  /** Leave, throwing the changes away */
  data object ConfirmDiscard : ItemEditorUiEvent
  data object CancelDiscard : ItemEditorUiEvent

  /** Throw the changes away and stay, editing the details the server has now */
  data object ResetChanges : ItemEditorUiEvent

  /** Keep the changes despite the server's details having changed meanwhile */
  data object KeepChanges : ItemEditorUiEvent

  /** Try fetching the details to edit again */
  data object RetryLoad : ItemEditorUiEvent

  data class ShowSection(val section: EditorSection) : ItemEditorUiEvent

  data object DismissFilledFrom : ItemEditorUiEvent
}
