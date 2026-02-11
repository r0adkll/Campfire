package app.campfire.playlists.ui.detail.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.thenIfNotNull
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.MotionPlay
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.ItemImage
import app.campfire.common.compose.widgets.LibraryItemSharedTransitionKey
import app.campfire.common.compose.widgets.OfflineStatusIndicator
import app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBox
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxValue
import app.campfire.common.compose.widgets.swipetodismiss.rememberSwipeToDismissBoxState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.offline.OfflineStatus
import com.slack.circuit.sharedelements.SharedElementTransitionScope
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun PlaylistListItem(
  item: LibraryItem,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier,
  handleModifier: Modifier = Modifier,
  sharedTransitionKey: String = item.id,
  sharedTransitionZIndex: Float = 0f,
  offlineStatus: OfflineStatus = OfflineStatus.None,
  isPlaying: Boolean = false,
  isDragging: Boolean = false,
  isReordering: Boolean = false,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  val swipeDismissState = rememberSwipeToDismissBoxState()
  SwipeToDismissBox(
    state = swipeDismissState,
    gesturesEnabled = !isReordering,
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
    val handlePadding by animateDpAsState(
      targetValue = when {
        isDragging -> 20.dp
        isReordering -> 12.dp
        else -> 8.dp
      },
    )

    Box(
      modifier = handleModifier
        .height(ThumbnailSize)
        .padding(horizontal = handlePadding),
      contentAlignment = Alignment.Center,
    ) {
      androidx.compose.animation.AnimatedVisibility(
        visible = isReordering,
      ) {
        Icon(Icons.Rounded.DragIndicator, contentDescription = "Drag handle")
      }
    }

    PlaylistItemContent(
      item = item,
      isPlaying = isPlaying,
      offlineStatus = offlineStatus,
      onClick = onClick,
      onPlayClick = onPlayClick,
      interactionSource = interactionSource,
      sharedTransitionKey = sharedTransitionKey,
      sharedTransitionZIndex = sharedTransitionZIndex,
      modifier = Modifier.weight(1f),
    )

    Spacer(Modifier.size(16.dp))
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
private fun PlaylistItemContent(
  item: LibraryItem,
  isPlaying: Boolean,
  offlineStatus: OfflineStatus,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
  sharedTransitionKey: String = item.id,
  sharedTransitionZIndex: Float = 0f,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) = SharedElementTransitionScope {
  val shape = MaterialTheme.shapes.large
  ElevatedCard(
    onClick = onClick,
    interactionSource = interactionSource,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    shape = shape,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box {
        val animationScope = findAnimatedScope(SharedElementTransitionScope.AnimatedScope.Navigation)

        ItemImage(
          imageUrl = item.media.coverImageUrl,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .thenIfNotNull(animationScope) { scope ->
              sharedElement(
                sharedContentState = rememberSharedContentState(
                  LibraryItemSharedTransitionKey(
                    id = sharedTransitionKey,
                    type = LibraryItemSharedTransitionKey.ElementType.Image,
                  ),
                ),
                animatedVisibilityScope = scope,
                zIndexInOverlay = sharedTransitionZIndex,
              )
            }
            .clip(shape)
            .size(ThumbnailSize),
        )

        OfflineStatusIndicator(
          status = offlineStatus,
          size = 20.dp,
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(8.dp),
        )
      }

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

      FilledTonalIconButton(
        enabled = !isPlaying,
        onClick = onPlayClick,
        shapes = IconButtonDefaults.shapes(
          shape = MaterialTheme.shapes.small,
          pressedShape = CircleShape,
        ),
      ) {
        AnimatedContent(
          isPlaying,
        ) { playing ->
          Icon(
            if (!playing) {
              Icons.Rounded.PlayArrow
            } else {
              CampfireIcons.Rounded.MotionPlay
            },
            contentDescription = null,
          )
        }
      }

      Spacer(Modifier.size(8.dp))
    }
  }
}

private val ThumbnailSize = 88.dp

@Preview
@Composable
fun PlaylistItemPreview() {
  CampfireTheme {
    CompositionLocalProvider() {
      Surface(Modifier.fillMaxSize()) {
        Column {
          PlaylistListItem(
            item = libraryItem("item_1"),
            onClick = {},
            onRemove = {},
            onPlayClick = {},
          )
          PlaylistListItem(
            item = libraryItem("item_2"),
            onClick = {},
            onRemove = {},
            onPlayClick = {},
            isReordering = true,
            isPlaying = true,
            offlineStatus = OfflineStatus.Downloading(0.4f),
          )
          PlaylistListItem(
            item = libraryItem("item_3"),
            onClick = {},
            onRemove = {},
            onPlayClick = {},
            isReordering = true,
            isDragging = true,
            offlineStatus = OfflineStatus.Available,
          )
        }
      }
    }
  }
}
