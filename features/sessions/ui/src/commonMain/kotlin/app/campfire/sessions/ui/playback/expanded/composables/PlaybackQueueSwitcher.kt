// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.QueueUiState

/**
 * Cross-fades between the queue and the player [content] beneath the top bar. Picking a queue
 * entry plays it and hands back to the player through [onQueueItemChosen].
 */
@Composable
internal fun PlaybackQueueSwitcher(
  showQueue: Boolean,
  queueState: QueueUiState,
  onQueueItemChosen: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  AnimatedContent(
    targetState = showQueue,
    modifier = modifier,
  ) { queue ->
    if (queue) {
      QueueContent(
        queue = queueState.queue,
        onItemClick = { entry ->
          queueState.eventSink(QueueUiEvent.QueueItemClick(entry))
          onQueueItemChosen()
        },
        onRemoveItem = { entry ->
          queueState.eventSink(QueueUiEvent.RemoveQueueItem(entry))
        },
        onReorderItem = { fromKey, toKey ->
          queueState.eventSink(QueueUiEvent.ReorderItem(fromKey, toKey))
        },
        onReorderStopped = {
          queueState.eventSink(QueueUiEvent.ReorderStopped)
        },
        modifier = Modifier.fillMaxSize(),
      )
    } else {
      content()
    }
  }
}
