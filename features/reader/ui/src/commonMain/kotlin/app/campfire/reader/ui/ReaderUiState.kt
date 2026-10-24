// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.reader.ui

import androidx.compose.runtime.Immutable
import app.campfire.core.coroutines.LoadState
import app.campfire.reader.ui.web.ReaderBridge
import app.campfire.reader.ui.web.ReaderContent
import app.campfire.reader.ui.web.ReaderStyle
import app.campfire.reader.ui.web.TocEntry
import app.campfire.settings.api.ReaderLayout
import app.campfire.settings.api.ReaderLayoutOverrides
import app.campfire.settings.api.ReaderTheme
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class ReaderUiState(
  val title: String,
  val author: String?,
  val content: LoadState<out ReaderContent>,
  val bridge: ReaderBridge,
  val chromeVisible: Boolean,
  val position: ReaderPosition,
  val toc: List<TocEntry>,
  /** Where the reader was before jumping through the contents or the scrubber. */
  val jumpBack: JumpBack?,
  /** Audio playing alongside the book. */
  val nowPlaying: NowPlaying?,
  /** Shown on the book's last page. */
  val endOfBook: EndOfBook?,
  val theme: ReaderTheme,
  /** The layout this book reads with: the one for all books, with its overrides. */
  val layout: ReaderLayout,
  val keepScreenOn: Boolean,
  val volumeKeysTurnPages: Boolean,
  /** The appearance panel, while it's open. */
  val appearance: AppearancePanel?,
  val bookError: String?,
  val eventSink: (ReaderUiEvent) -> Unit,
) : CircuitUiState

@Immutable
data class ReaderPosition(
  /** The part [chapter] is in, where the contents nest chapters in parts. */
  val part: String? = null,
  val chapter: String? = null,
  /** The [TocEntry.href] of the chapter. */
  val chapterHref: String? = null,
  /** How far through the book, 0..1. */
  val progress: Float = 0f,
  /** Pages after this one in the chapter, where the book reflows into pages. */
  val pagesLeftInChapter: Int? = null,
)

@Immutable
data class JumpBack(
  val location: String,
  val progress: Float,
)

@Immutable
data class NowPlaying(
  val title: String,
  val isPlaying: Boolean,
)

@Immutable
data class EndOfBook(
  val isFinished: Boolean,
)

/** Which books the appearance panel changes. */
enum class AppearanceScope { AllBooks, ThisBook }

@Immutable
data class AppearancePanel(
  val scope: AppearanceScope,
  /** The layout the panel shows and edits: for all books, or this book's. */
  val layout: ReaderLayout,
  /** Where this book differs from all books. */
  val overrides: ReaderLayoutOverrides,
) {
  val hasOverrides: Boolean get() = !overrides.isEmpty
}

sealed interface ReaderUiEvent {
  data object Back : ReaderUiEvent
  data object ToggleChrome : ReaderUiEvent
  data class GoTo(val entry: TocEntry) : ReaderUiEvent

  /** Jump to [fraction] of the way through the book, 0..1. */
  data class Scrub(val fraction: Float) : ReaderUiEvent
  data object JumpBack : ReaderUiEvent
  data object PlayPause : ReaderUiEvent
  data object MarkFinished : ReaderUiEvent
  data object DismissEndOfBook : ReaderUiEvent
  data class StyleChanged(val style: ReaderStyle) : ReaderUiEvent
  data object NextPage : ReaderUiEvent
  data object PreviousPage : ReaderUiEvent

  data object ShowAppearance : ReaderUiEvent
  data object HideAppearance : ReaderUiEvent
  data class AppearanceScopeChanged(val scope: AppearanceScope) : ReaderUiEvent
  data class ThemeChanged(val theme: ReaderTheme) : ReaderUiEvent
  data class LayoutChanged(val change: ReaderLayoutChange) : ReaderUiEvent

  /** Drop this book's own settings, so it reads like all the others. */
  data object ResetBookLayout : ReaderUiEvent

  /** Put every book's theme and layout back as they were to begin with. */
  data object ResetToDefaults : ReaderUiEvent
  data class KeepScreenOnChanged(val keepOn: Boolean) : ReaderUiEvent
  data class VolumeKeysChanged(val enabled: Boolean) : ReaderUiEvent
}
