package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Immutable
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Session
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class LibraryItemUiState(
  val libraryItem: LibraryItem?,
  val contentState: LoadState<out List<ContentSlot>>,
  val showConfirmDownloadDialog: Boolean,
  val eventSink: (LibraryItemUiEvent) -> Unit,
) : CircuitUiState

sealed interface SessionUiState {
  data object None : SessionUiState
  data class Current(val session: Session) : SessionUiState

  fun sessionOrNull(): Session? = (this as? Current)?.session
}

sealed interface LibraryItemUiEvent : CircuitUiEvent {
  data class PlayClick(val item: LibraryItem) : LibraryItemUiEvent
  data class SeriesClick(val item: LibraryItem) : LibraryItemUiEvent
  data class DiscardProgress(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkNotFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class AuthorClick(val item: LibraryItem) : LibraryItemUiEvent
  data class NarratorClick(val item: LibraryItem) : LibraryItemUiEvent
  data class ChapterClick(val item: LibraryItem, val chapter: Chapter) : LibraryItemUiEvent
  data class TimeInBookChange(val enabled: Boolean) : LibraryItemUiEvent

  data class DownloadClick(val doNotShowAgain: Boolean = true) : LibraryItemUiEvent
  data object RemoveDownloadClick : LibraryItemUiEvent
  data object StopDownloadClick : LibraryItemUiEvent

  data object OnBack : LibraryItemUiEvent
}
