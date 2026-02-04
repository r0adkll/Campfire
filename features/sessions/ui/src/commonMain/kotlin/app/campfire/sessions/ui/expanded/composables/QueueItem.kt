package app.campfire.sessions.ui.expanded.composables

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.rounded.rememberMovingDeletePainter
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.core.animations.lerp
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItem

private val ThumbnailSize = 88.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun QueueItem(
  item: LibraryItem,
  onClick: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val swipeDismissState = rememberSwipeToDismissBoxState()
  SwipeToDismissBox(
    state = swipeDismissState,
    enableDismissFromStartToEnd = false,
    onDismiss = {
      if (it != SwipeToDismissBoxValue.Settled) {
        onRemove()
      }
    },
    backgroundContent = {
      Spacer(Modifier.weight(1f))
      QueueItemBackgroundContent(swipeDismissState)
    },
    modifier = modifier,
  ) {
    QueueItemContent(item, onClick)
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QueueItemContent(
  item: LibraryItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.large
  ElevatedCard(
    onClick = onClick,
    modifier = modifier,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    shape = shape,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {

      ItemImage(
        imageUrl = item.media.coverImageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
          .clip(shape)
          .size(ThumbnailSize),
      )

      Spacer(Modifier.size(16.dp))

      Column(
        modifier = Modifier
          .weight(1f),
      ) {
        Text(
          text = item.media.metadata.title ?: "Unknown",
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = item.media.metadata.authorName ?: "--",
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.size(4.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Icon(
            Icons.Outlined.Schedule,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = item.media.duration.thresholdReadoutFormat(),
            style = MaterialTheme.typography.labelSmallEmphasized,
          )
        }
      }
      Spacer(Modifier.size(16.dp))
    }
  }
}

@Composable
private fun RowScope.QueueItemBackgroundContent(
  state: SwipeToDismissBoxState,
  modifier: Modifier = Modifier,
) {
  val actualProgress = state
    .progress(SwipeToDismissBoxValue.Settled, SwipeToDismissBoxValue.EndToStart)
  val easedProgress = EaseOutCubic.transform(actualProgress)

  val rotation = lerp(0f, 30f, easedProgress)
  val offset = androidx.compose.ui.unit.lerp(0.dp, 16.dp, easedProgress)


  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(state.targetValue) {
    if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
      hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  Icon(
    rememberMovingDeletePainter(rotation),
    contentDescription = null,
    tint = MaterialTheme.colorScheme.error,
    modifier = modifier
      .align(Alignment.CenterVertically)
      .size(56.dp)
      .offset {
        IntOffset(-offset.roundToPx(), 0)
      },
  )
}
