// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.management.ui.cover

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import app.campfire.management.api.cover.BookImage
import app.campfire.management.api.match.ImageSize
import app.campfire.management.api.match.MatchProvider
import app.campfire.management.ui.editor.StagedCover
import com.slack.circuit.runtime.CircuitUiEvent

@Stable
data class CoverUiState(
  /** The book's cover as the server has it, or null when it has none */
  val currentUrl: String?,
  /** How big the book's cover is, once its file says */
  val currentSize: ImageSize?,
  /** The cover chosen to take the place of the book's own once the details are saved */
  val staged: StagedCover?,
  val mode: CoverMode,
  /** Where covers can be searched for, empty until the server's sources have loaded */
  val sources: List<CoverSource>,
  val source: CoverSource?,
  val title: TextFieldState,
  val author: TextFieldState,
  /** Each source searched, in order, with what it found */
  val results: List<CoverResults>,
  /** Covers' sizes by address, as their files say */
  val sizes: Map<String, ImageSize>,
  /** An image's address, typed or pasted */
  val address: TextFieldState,
  val bookImages: BookImages,
  /**
   * Whether the account may give the book a cover from an address, whether a source's or typed,
   * which takes the upload permission
   */
  val canUseAddresses: Boolean,
  /** Whether the account may take the book's cover away, which takes the delete permission */
  val canRemove: Boolean,
  /** The cover being looked at closely before it's chosen */
  val preview: CoverPreview?,
  val eventSink: (CoverUiEvent) -> Unit,
)

/** Where a new cover comes from */
enum class CoverMode {
  Search,
  Address,
  BookFiles,
}

@Immutable
sealed interface CoverSource {

  /** The sources the server finds covers best in, searched side by side */
  data object Best : CoverSource

  data class Single(val provider: MatchProvider) : CoverSource
}

/** What one source found */
@Immutable
data class CoverResults(
  val provider: MatchProvider,
  val search: CoverSearch,
)

@Immutable
sealed interface CoverSearch {
  data object Searching : CoverSearch
  data class Found(val covers: List<String>) : CoverSearch
  data object Failed : CoverSearch
}

/** The images among the book's own files */
@Immutable
sealed interface BookImages {
  data object Loading : BookImages
  data class Loaded(val images: List<BookImage>) : BookImages
  data object Failed : BookImages
}

/** A cover to look at closely, one of [covers] to page through */
@Immutable
data class CoverPreview(
  val covers: List<String>,
  val index: Int,
  /** How big it is, once its file says */
  val size: ImageSize? = null,
) {
  val url: String = covers[index]
}

sealed interface CoverUiEvent : CircuitUiEvent {
  data class ChooseMode(val mode: CoverMode) : CoverUiEvent
  data class ChooseSource(val source: CoverSource) : CoverUiEvent
  data object Search : CoverUiEvent

  /** Look closely at a cover a source found */
  data class Preview(val url: String) : CoverUiEvent

  /** Look closely at the image at the address typed in */
  data object PreviewAddress : CoverUiEvent

  /** Page to another of the covers being looked at */
  data class PageTo(val index: Int) : CoverUiEvent
  data object ClosePreview : CoverUiEvent

  /** Choose the cover being looked at, to take the book's own place once saved */
  data object UsePreviewed : CoverUiEvent

  /** Choose one of the book's own images as its cover */
  data class UseBookImage(val image: BookImage) : CoverUiEvent
  data object RetryBookImages : CoverUiEvent

  /** Choose to take the book's cover away once saved */
  data object Remove : CoverUiEvent

  /** Keep the book's cover after all */
  data object KeepCurrent : CoverUiEvent
}
