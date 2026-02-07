package app.campfire.sessions.ui.expanded

import androidx.compose.runtime.Stable
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Stable
data class ExpandedPlaybackUiState(
  val queue: List<LibraryItem>,
  val reorderSink: suspend (fromItemId: LibraryItemId, toItemId: LibraryItemId) -> Unit,
  val eventSink: (ExpandedPlaybackUiEvent) -> Unit,
) : CircuitUiState

sealed interface ExpandedPlaybackUiEvent : CircuitUiEvent {
  data class QueueItemClick(val item: LibraryItem) : ExpandedPlaybackUiEvent
  data class RemoveQueueItem(val item: LibraryItem) : ExpandedPlaybackUiEvent
}
