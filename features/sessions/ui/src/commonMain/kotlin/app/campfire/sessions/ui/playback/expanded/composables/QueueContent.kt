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
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
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
  queue: List<LibraryItem>,
  onItemClick: (LibraryItem) -> Unit,
  onRemoveItem: (LibraryItem) -> Unit,
  onReorderItem: suspend (from: LibraryItemId, to: LibraryItemId) -> Unit,
  onReorderStopped: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    onReorderItem(from.key as LibraryItemId, to.key as LibraryItemId)
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
    queue.groupBy { queue.indexOf(it) == 0 }.forEach { (isFirst, items) ->
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
        items = items,
        key = { it.id },
      ) { item ->
        ReorderableItem(reorderableLazyListState, key = item.id) {
          val interactionSource = remember { MutableInteractionSource() }
          QueueItem(
            item = item,
            onClick = { onItemClick(item) },
            onRemove = { onRemoveItem(item) },
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
