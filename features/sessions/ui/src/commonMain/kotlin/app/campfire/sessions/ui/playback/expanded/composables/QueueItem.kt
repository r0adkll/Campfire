// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded.composables

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBox
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxValue
import app.campfire.common.compose.widgets.swipetodismiss.rememberSwipeToDismissBoxState
import app.campfire.sessions.api.QueuedEntry

private val ThumbnailSize = 88.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun QueueItem(
  entry: QueuedEntry,
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
      AnimatedRemoveBackgroundContent(swipeDismissState)
    },
    modifier = modifier,
  ) {
    QueueItemContent(
      entry = entry,
      onClick = onClick,
      interactionSource = interactionSource,
    )
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QueueItemContent(
  entry: QueuedEntry,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  // For podcast entries the playable unit is the episode — surface its title and duration
  // while the cover/author still come from the parent podcast.
  val title = entry.episode?.title ?: entry.libraryItem.media.metadata.title ?: "Unknown"
  val subtitle = entry.libraryItem.media.metadata.title?.takeIf { entry.episode != null }
    ?: entry.libraryItem.media.metadata.authorName ?: "--"
  val duration = entry.episode?.duration ?: entry.libraryItem.media.duration

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
        imageUrl = entry.libraryItem.media.coverImageUrl,
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
          text = title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = subtitle,
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
            text = duration.thresholdReadoutFormat(),
            style = MaterialTheme.typography.labelSmallEmphasized,
          )
        }
      }
      Spacer(Modifier.size(16.dp))
    }
  }
}
