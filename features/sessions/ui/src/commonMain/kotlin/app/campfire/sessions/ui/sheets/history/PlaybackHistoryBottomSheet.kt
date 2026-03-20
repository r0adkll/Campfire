package app.campfire.sessions.ui.sheets.history

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.core.di.UserScope
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.sessions.ui.sheets.rememberSessionSheetTitleState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.history_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.history_context_pause
import campfire.features.sessions.ui.generated.resources.history_context_play
import campfire.features.sessions.ui.generated.resources.history_context_seek
import campfire.features.sessions.ui.generated.resources.history_context_seek_backward
import campfire.features.sessions.ui.generated.resources.history_context_seek_forward
import campfire.features.sessions.ui.generated.resources.history_context_skip_next
import campfire.features.sessions.ui.generated.resources.history_context_skip_previous
import campfire.features.sessions.ui.generated.resources.history_empty_message
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface PlaybackHistoryBottomSheetComponent {
  val playbackHistoryRecorder: PlaybackHistoryRecorder
}

suspend fun OverlayHost.showPlaybackHistoryBottomSheet(
  libraryItemId: LibraryItemId,
) {
  show(
    BottomSheetOverlay<LibraryItemId, Unit>(
      model = libraryItemId,
      onDismiss = { },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      dragHandle = {},
    ) { model, _ ->
      PlaybackHistoryBottomSheet(
        libraryItemId = model,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@Composable
private fun PlaybackHistoryBottomSheet(
  libraryItemId: LibraryItemId,
  modifier: Modifier = Modifier,
  component: PlaybackHistoryBottomSheetComponent = rememberComponent(),
) {
  val actions by remember(libraryItemId) {
    component.playbackHistoryRecorder.observeActions(libraryItemId)
  }.collectAsState(emptyList())

  val sessionSheetState = rememberSessionSheetTitleState()

  SessionSheetLayout(
    modifier = modifier,
    state = sessionSheetState,
    title = {
      Text(stringResource(Res.string.history_bottomsheet_title))
    },
  ) {
    if (actions.isEmpty()) {
      ListItem(
        headlineContent = {
          Text(
            text = stringResource(Res.string.history_empty_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      )
    } else {
      val lazyListState = rememberLazyListState()

      val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
      }

      LaunchedEffect(isScrolled) {
        sessionSheetState.isScrolled = isScrolled
      }

      LazyColumn(
        state = lazyListState,
      ) {
        items(
          items = actions,
          key = { it.id },
        ) { action ->
          PlaybackHistoryItem(action)
        }
      }
    }
  }
}

@Composable
private fun PlaybackHistoryItem(
  action: PlaybackAction,
  modifier: Modifier = Modifier,
) {
  ListItem(
    leadingContent = {
      Icon(
        imageVector = action.type.icon(),
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = action.type.iconTint(),
      )
    },
    headlineContent = {
      Text(
        text = action.type.contextString(),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
      )
    },
    supportingContent = {
      Text(
        text = action.timestamp.formatTime(),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    trailingContent = {
      Text(
        text = action.positionContext(),
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    },
    modifier = modifier,
    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
  )
}

@Composable
private fun PlaybackActionType.icon(): ImageVector = when (this) {
  PlaybackActionType.Play -> Icons.Rounded.PlayArrow
  PlaybackActionType.Pause -> Icons.Rounded.Pause
  PlaybackActionType.Seek -> Icons.AutoMirrored.Rounded.ArrowForward
  PlaybackActionType.SkipNext -> Icons.Rounded.SkipNext
  PlaybackActionType.SkipPrevious -> Icons.Rounded.SkipPrevious
  PlaybackActionType.SeekForward -> Icons.Rounded.FastForward
  PlaybackActionType.SeekBackward -> Icons.Rounded.FastRewind
}

@Composable
private fun PlaybackActionType.iconTint(): Color = when (this) {
  PlaybackActionType.Play -> MaterialTheme.colorScheme.primary
  PlaybackActionType.Pause -> MaterialTheme.colorScheme.secondary
  PlaybackActionType.Seek -> MaterialTheme.colorScheme.tertiary
  PlaybackActionType.SkipNext -> MaterialTheme.colorScheme.primary
  PlaybackActionType.SkipPrevious -> MaterialTheme.colorScheme.primary
  PlaybackActionType.SeekForward -> MaterialTheme.colorScheme.tertiary
  PlaybackActionType.SeekBackward -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun PlaybackActionType.contextString(): String = when (this) {
  PlaybackActionType.Play -> stringResource(Res.string.history_context_play)
  PlaybackActionType.Pause -> stringResource(Res.string.history_context_pause)
  PlaybackActionType.Seek -> stringResource(Res.string.history_context_seek)
  PlaybackActionType.SkipNext -> stringResource(Res.string.history_context_skip_next)
  PlaybackActionType.SkipPrevious -> stringResource(Res.string.history_context_skip_previous)
  PlaybackActionType.SeekForward -> stringResource(Res.string.history_context_seek_forward)
  PlaybackActionType.SeekBackward -> stringResource(Res.string.history_context_seek_backward)
}

private fun PlaybackAction.positionContext(): String {
  return when (type) {
    PlaybackActionType.Seek,
    PlaybackActionType.SeekForward,
    PlaybackActionType.SeekBackward,
    PlaybackActionType.SkipNext,
    PlaybackActionType.SkipPrevious,
    -> "${fromPosition.clockFormat()} → ${toPosition.clockFormat()}"

    else -> toPosition.clockFormat()
  }
}

private val TimeFormat = LocalDateTime.Format {
  hour()
  char(':')
  minute()
  char(':')
  second()
}

private fun LocalDateTime.formatTime(): String = format(TimeFormat)
