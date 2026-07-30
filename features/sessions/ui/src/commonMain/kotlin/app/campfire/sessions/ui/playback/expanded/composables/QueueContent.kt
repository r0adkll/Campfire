// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.sessions.api.QueuedEntry
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.queue_header_queue
import campfire.features.sessions.ui.generated.resources.queue_header_up_next
import kotlin.collections.component1
import kotlin.collections.component2
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
internal fun QueueContent(
  queue: List<QueuedEntry>,
  onItemClick: (QueuedEntry) -> Unit,
  onRemoveItem: (QueuedEntry) -> Unit,
  onReorderItem: suspend (fromKey: String, toKey: String) -> Unit,
  onReorderStopped: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    onReorderItem(from.key as String, to.key as String)
    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
  }
  LazyColumn(
    modifier = modifier,
    state = lazyListState,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(
      horizontal = 16.dp,
    ),
  ) {
    queue.groupBy { queue.indexOf(it) == 0 }.forEach { (isFirst, entries) ->
      if (isFirst) {
        item {
          MetadataHeader(
            title = stringResource(Res.string.queue_header_up_next),
          )
        }
      } else {
        item {
          MetadataHeader(
            title = stringResource(Res.string.queue_header_queue),
          )
        }
      }

      items(
        items = entries,
        key = { it.key },
      ) { entry ->
        ReorderableItem(reorderableLazyListState, key = entry.key) {
          val interactionSource = remember { MutableInteractionSource() }
          QueueItem(
            entry = entry,
            onClick = { onItemClick(entry) },
            onRemove = { onRemoveItem(entry) },
            interactionSource = interactionSource,
            modifier = Modifier
              .animateItem()
              .longPressDraggableHandle(
                onDragStarted = {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragStopped = {
                  haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                  onReorderStopped()
                },
                interactionSource = interactionSource,
              ),
          )
        }
      }
    }
  }
}
