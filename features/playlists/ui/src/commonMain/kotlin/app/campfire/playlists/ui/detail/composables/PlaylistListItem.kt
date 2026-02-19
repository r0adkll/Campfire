package app.campfire.playlists.ui.detail.composables

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.MotionPlay
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.LibraryItemListItem
import app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBox
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxValue
import app.campfire.common.compose.widgets.swipetodismiss.rememberSwipeToDismissBoxState
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.offline.OfflineStatus
import com.slack.circuit.sharedelements.PreviewSharedElementTransitionLayout
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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

    LibraryItemListItem(
      libraryItem = item,
      onClick = onClick,
      offlineStatus = offlineStatus,
      interactionSource = interactionSource,
      sharedTransitionKey = sharedTransitionKey,
      sharedTransitionZIndex = sharedTransitionZIndex,
      modifier = Modifier.weight(1f),
      trailingContent = {
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
      },
    )

    Spacer(Modifier.size(16.dp))
  }
}

private val ThumbnailSize = 88.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Preview
@Composable
fun PlaylistItemPreview() {
  CampfireTheme {
    PreviewSharedElementTransitionLayout {
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
}
