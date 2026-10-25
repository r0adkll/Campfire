// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.match

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.management.api.match.BookMatch
import app.campfire.management.api.match.ImageSize
import app.campfire.management.api.match.MatchProvider
import com.slack.circuit.runtime.CircuitUiEvent
import kotlin.time.Duration

@Stable
data class MatchUiState(
  /** The sources to search, empty until they've loaded */
  val providers: List<MatchProvider>,
  val provider: MatchProvider?,
  /** A title, or an ASIN */
  val title: TextFieldState,
  val author: TextFieldState,
  val search: MatchSearch,
  /** How long the book is, to tell results of another length apart */
  val bookDuration: Duration?,
  val bookAsin: String?,
  /** The result being compared with the book, if one is */
  val review: MatchReviewState?,
  /** Whether the account may give the book a new cover, which takes the upload permission */
  val canChangeCover: Boolean,
  val eventSink: (MatchUiEvent) -> Unit,
)

@Immutable
sealed interface MatchSearch {
  data object NotSearched : MatchSearch
  data object Searching : MatchSearch
  data class Found(val provider: MatchProvider, val matches: List<BookMatch>) : MatchSearch

  /** The search, or listing the sources to search, didn't work out */
  data class Failed(val provider: MatchProvider?) : MatchSearch
}

/** A result beside the book, with the details chosen to take from it */
@Immutable
data class MatchReviewState(
  val match: BookMatch,
  val providerName: String,
  val rows: List<MatchRow>,
  val selected: Set<MatchField>,
  val merges: Map<MatchField, ListMerge>,
  val canChangeCover: Boolean,
  /** How big the book's cover is, once its file says */
  val currentCoverSize: ImageSize? = null,
  /** How big the result's cover is, once its file says */
  val newCoverSize: ImageSize? = null,
) {
  /** The details the result would change */
  val changing: List<MatchRow> = rows.filterNot { it.isSame }

  /** The result's cover beside the book's, if it has one, which the review shows apart */
  val cover: MatchRow? = changing.firstOrNull { it.field == MatchField.Cover }

  /** The details the result would change, but the cover */
  val details: List<MatchRow> = changing.filterNot { it.field == MatchField.Cover }

  /** The details the book already has as the result does */
  val unchanged: List<MatchRow> = rows.filter { it.isSame }

  /** The way of choosing that would pick exactly what's chosen, if one would */
  val mode: SelectionMode? = SelectionMode.entries.firstOrNull { rows.selectedBy(it, canChangeCover) == selected }
}

sealed interface MatchUiEvent : CircuitUiEvent {
  data class ChooseProvider(val provider: MatchProvider) : MatchUiEvent
  data object Search : MatchUiEvent
  data class Review(val match: BookMatch) : MatchUiEvent
  data object CloseReview : MatchUiEvent
  data class Select(val mode: SelectionMode) : MatchUiEvent
  data class Toggle(val field: MatchField) : MatchUiEvent
  data class Merge(val field: MatchField, val merge: ListMerge) : MatchUiEvent

  /** Fill the form with the chosen details, to look over and save */
  data object ApplyToForm : MatchUiEvent

  /** Fill the form with the chosen details and save them */
  data object ApplyAndSave : MatchUiEvent
}
