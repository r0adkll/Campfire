// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.queue

import androidx.compose.runtime.Immutable
import app.campfire.sessions.api.QueuedEntry
import app.campfire.sessions.ui.playback.QueueUiState
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState

@Immutable
data class QueueScreenUiState(
  /** What's in the player, shown above the queue; null with nothing playing. */
  val nowPlaying: QueuedEntry?,
  val queueState: QueueUiState,
  val eventSink: (QueueScreenUiEvent) -> Unit,
) : CircuitUiState

sealed interface QueueScreenUiEvent : CircuitUiEvent {
  data object Back : QueueScreenUiEvent

  /** Starts the first queued entry, for when nothing is playing. */
  data object PlayQueue : QueueScreenUiEvent

  data class OpenItem(val entry: QueuedEntry) : QueueScreenUiEvent
}
