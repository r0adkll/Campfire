package app.campfire.libraries.ui.detail

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import app.campfire.core.coroutines.LoadState
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Session
import app.campfire.core.model.User
import app.campfire.libraries.ui.detail.composables.slots.ContentSlot
import com.r0adkll.swatchbuckler.compose.Swatch
import com.r0adkll.swatchbuckler.compose.Theme
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class LibraryItemUiState(
  val user: User,
  val libraryItem: LibraryItem?,
  val swatch: Swatch? = null,
  val theme: Theme? = null,
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
  data class SeedColorChange(val seedColor: Color) : LibraryItemUiEvent

  data class PlayClick(val item: LibraryItem) : LibraryItemUiEvent
  data class SeriesClick(val item: LibraryItem) : LibraryItemUiEvent
  data class DiscardProgress(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class MarkNotFinished(val item: LibraryItem) : LibraryItemUiEvent
  data class AuthorClick(val item: LibraryItem, val author: String) : LibraryItemUiEvent
  data class NarratorClick(val item: LibraryItem, val narrator: String) : LibraryItemUiEvent
  data class ChapterClick(val item: LibraryItem, val chapter: Chapter) : LibraryItemUiEvent
  data class AudioTrackClick(val item: LibraryItem, val track: AudioTrack) : LibraryItemUiEvent
  data class TimeInBookChange(val enabled: Boolean) : LibraryItemUiEvent

  data class DownloadClick(val doNotShowAgain: Boolean = true) : LibraryItemUiEvent
  data object RemoveDownloadClick : LibraryItemUiEvent
  data object StopDownloadClick : LibraryItemUiEvent

  data object OnBack : LibraryItemUiEvent
}
