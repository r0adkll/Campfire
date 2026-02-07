package app.campfire.sessions.ui.expanded.composables

import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.rounded.rememberMovingDeletePainter
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.core.animations.lerp
import app.campfire.core.model.LibraryItem
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_remove
import org.jetbrains.compose.resources.stringResource

private val ThumbnailSize = 88.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun QueueItem(
  item: LibraryItem,
  onClick: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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
    QueueItemContent(
      item = item,
      onClick = onClick,
      interactionSource = interactionSource,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QueueItemContent(
  item: LibraryItem,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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
    interactionSource = interactionSource,
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

private const val DefaultTextScale = 0.85f

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RowScope.QueueItemBackgroundContent(
  state: SwipeToDismissBoxState,
  modifier: Modifier = Modifier,
) {
  val actualProgress = state
    .progress(SwipeToDismissBoxValue.Settled, SwipeToDismissBoxValue.EndToStart)
    .times(2f)
    .coerceIn(0f, 1f)
  val inverseEasedProgress = EaseInCubic.transform(actualProgress)

  val rotation = lerp(0f, 20f, inverseEasedProgress)
  val offset = androidx.compose.ui.unit.lerp(0.dp, 8.dp, inverseEasedProgress)
  var scale by remember { mutableFloatStateOf(1f) }

  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(state.targetValue) {
    if (state.targetValue == SwipeToDismissBoxValue.EndToStart) {
      hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)

      animate(1f, 1.2f) { value, _ -> scale = value }
      animate(
        initialValue = 1.2f,
        targetValue = 1f,
        animationSpec = spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessLow,
        ),
      ) { value, _ ->
        scale = value
      }
    }
  }

  Row(
    modifier = Modifier
      .align(Alignment.CenterVertically)
      .padding(
        end = 4.dp,
      )
      .scale(scale)
      .offset {
        IntOffset(-offset.roundToPx(), 0)
      },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(Res.string.action_remove),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.error,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .alpha(inverseEasedProgress),
    )

    Icon(
      rememberMovingDeletePainter(rotation),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = modifier.size(56.dp),
    )
  }
}
