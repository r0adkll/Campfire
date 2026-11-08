// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.toMutableStateList
import app.campfire.audioplayer.PlaybackController
import app.campfire.sessions.api.SessionQueue
import kotlinx.coroutines.launch

/**
 * The playback queue as the player and the Queue screen show it. Drags reorder a local copy
 * so the list follows the finger, and the new order is saved when the drag stops.
 */
@Composable
internal fun presentQueueState(
  sessionQueue: SessionQueue,
  playbackController: PlaybackController,
): QueueUiState {
  val scope = rememberCoroutineScope()

  val queue by remember {
    sessionQueue.observeAll()
  }.collectAsState(emptyList())

  val localQueue = remember(queue) {
    queue.toMutableStateList()
  }

  return QueueUiState(
    queue = localQueue,
  ) { event ->
    when (event) {
      is QueueUiEvent.ReorderItem -> {
        val fromIndex = localQueue.indexOfFirst { it.key == event.fromKey }
        val toIndex = localQueue.indexOfFirst { it.key == event.toKey }
        if (fromIndex >= 0 && toIndex >= 0) {
          localQueue.add(toIndex, localQueue.removeAt(fromIndex))
        }
      }

      QueueUiEvent.ReorderStopped -> scope.launch {
        sessionQueue.reorder(localQueue.toList())
      }

      QueueUiEvent.ClearQueue -> scope.launch {
        sessionQueue.clear()
      }

      is QueueUiEvent.QueueItemClick -> {
        playbackController.startSession(
          itemId = event.entry.libraryItemId,
          episodeId = event.entry.episodeId,
        )
        scope.launch {
          sessionQueue.remove(event.entry.libraryItemId, event.entry.episodeId)
        }
      }

      is QueueUiEvent.RemoveQueueItem -> scope.launch {
        sessionQueue.remove(event.entry.libraryItemId, event.entry.episodeId)
      }
    }
  }
}
