package app.campfire.libraries.ui.detail

import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Session
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

data class LibraryItemUiState(
  val sessionUiState: SessionUiState,
  val libraryItemContentState: LoadState<out LibraryItem>,
  val seriesContentState: LoadState<out List<LibraryItem>>,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface SessionUiState {
  data object None : SessionUiState
  data class Current(val session: Session) : SessionUiState
}

sealed class LibraryItemContentState {
  data object Loading : LibraryItemContentState()
  data class Loaded(val item: LibraryItem) : LibraryItemContentState()
  data object Error : LibraryItemContentState()
}

sealed interface LibraryItemUiEvent : CircuitUiEvent {
  data class PlayClick(val item: LibraryItem) : LibraryItemUiEvent
  data class SeriesClick(val item: LibraryItem) : LibraryItemUiEvent
  data class DiscardProgress(val item: LibraryItem) : LibraryItemUiEvent
  data class ChapterClick(val item: LibraryItem, val chapter: Chapter) : LibraryItemUiEvent

  data object OnBack : LibraryItemUiEvent
}
