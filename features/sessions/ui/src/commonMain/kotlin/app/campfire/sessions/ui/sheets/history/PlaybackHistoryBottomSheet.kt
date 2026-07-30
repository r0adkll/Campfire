// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.sheets.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.Sync
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.sessions.ui.sheets.rememberSessionSheetTitleState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_clear_history
import campfire.features.sessions.ui.generated.resources.history_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.history_context_pause
import campfire.features.sessions.ui.generated.resources.history_context_play
import campfire.features.sessions.ui.generated.resources.history_context_seek
import campfire.features.sessions.ui.generated.resources.history_context_seek_backward
import campfire.features.sessions.ui.generated.resources.history_context_seek_forward
import campfire.features.sessions.ui.generated.resources.history_context_skip_next
import campfire.features.sessions.ui.generated.resources.history_context_skip_previous
import campfire.features.sessions.ui.generated.resources.history_context_sync
import campfire.features.sessions.ui.generated.resources.history_empty_message
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuitx.overlays.BottomSheetOverlay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface PlaybackHistoryBottomSheetComponent {
  val playbackHistoryRepository: PlaybackHistoryRepository
}

sealed interface PlaybackHistoryResult {
  data object None : PlaybackHistoryResult
  data class Selected(val action: PlaybackAction) : PlaybackHistoryResult
}

suspend fun OverlayHost.showPlaybackHistoryBottomSheet(
  libraryItemId: LibraryItemId,
): PlaybackHistoryResult {
  return show(
    BottomSheetOverlay<LibraryItemId, PlaybackHistoryResult>(
      model = libraryItemId,
      onDismiss = { PlaybackHistoryResult.None },
      sheetShape = RoundedCornerShape(
        topStart = 32.dp,
        topEnd = 32.dp,
      ),
      dragHandle = {},
    ) { model, overlayNavigator ->
      PlaybackHistoryBottomSheet(
        libraryItemId = model,
        navigator = overlayNavigator,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

@Composable
private fun PlaybackHistoryBottomSheet(
  libraryItemId: LibraryItemId,
  modifier: Modifier = Modifier,
  navigator: OverlayNavigator<PlaybackHistoryResult>,
  component: PlaybackHistoryBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()

  val actions by remember(libraryItemId) {
    component.playbackHistoryRepository.observe(libraryItemId)
  }.collectAsState(emptyList())

  val sessionSheetState = rememberSessionSheetTitleState()

  SessionSheetLayout(
    modifier = modifier,
    state = sessionSheetState,
    title = {
      Text(stringResource(Res.string.history_bottomsheet_title))
    },
    subtitle = {
      Text(
        text = "Tap an event to jump to that position",
        style = MaterialTheme.typography.labelMedium,
        fontStyle = FontStyle.Italic,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.tertiary,
      )
    },
    trailingContent = {
      AnimatedVisibility(
        visible = actions.isNotEmpty(),
        modifier = Modifier.align(Alignment.CenterEnd),
      ) {
        val clearHistoryLabel = stringResource(Res.string.action_clear_history)
        IconButtonTooltip(text = clearHistoryLabel) {
          IconButton(
            onClick = {
              scope.launch {
                component.playbackHistoryRepository.clear(libraryItemId)
              }
            },
          ) {
            Icon(
              CampfireIcons.Rounded.DeleteSweep,
              contentDescription = clearHistoryLabel,
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    },
  ) {
    if (actions.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .height(256.dp)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Icon(
          Icons.Rounded.HistoryEdu,
          contentDescription = null,
          modifier = Modifier.size(40.dp),
        )

        Text(
          text = stringResource(Res.string.history_empty_message),
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      val lazyListState = rememberLazyListState()

      val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
      }

      LaunchedEffect(isScrolled) {
        sessionSheetState.isScrolled = isScrolled
      }

      val groupedActions = remember(actions) {
        actions.groupBy { it.timestamp.date }
      }

      LazyColumn(
        state = lazyListState,
      ) {
        groupedActions.forEach { (date, dayActions) ->
          item(key = date) {
            Box(
              modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
              contentAlignment = Alignment.CenterStart,
            ) {
              Text(
                text = date.relativeDayLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
              )
            }
          }

          items(
            items = dayActions,
            key = { it.id },
          ) { action ->
            PlaybackHistoryItem(
              action = action,
              modifier = Modifier.clickable {
                navigator.finish(
                  PlaybackHistoryResult.Selected(action),
                )
              },
            )
          }
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
      Text(action.type.contextString())
    },
    supportingContent = {
      Text(action.timestamp.readableFormat)
    },
    trailingContent = {
      Text(
        text = action.positionContext(),
        style = MaterialTheme.typography.labelLarge,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
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
  PlaybackActionType.Sync -> CampfireIcons.Rounded.Sync
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
  PlaybackActionType.Sync -> MaterialTheme.colorScheme.primary
}

@Composable
private fun PlaybackActionType.contextString(): String {
  val string = when (this) {
    PlaybackActionType.Play -> stringResource(Res.string.history_context_play)
    PlaybackActionType.Pause -> stringResource(Res.string.history_context_pause)
    PlaybackActionType.Seek -> stringResource(Res.string.history_context_seek)
    PlaybackActionType.SkipNext -> stringResource(Res.string.history_context_skip_next)
    PlaybackActionType.SkipPrevious -> stringResource(Res.string.history_context_skip_previous)
    PlaybackActionType.SeekForward -> stringResource(Res.string.history_context_seek_forward)
    PlaybackActionType.SeekBackward -> stringResource(Res.string.history_context_seek_backward)
    PlaybackActionType.Sync -> stringResource(Res.string.history_context_sync)
  }
  return string
}

private fun PlaybackAction.positionContext(): String {
  return when (type) {
    PlaybackActionType.Seek,
    PlaybackActionType.SeekForward,
    PlaybackActionType.SeekBackward,
    PlaybackActionType.SkipNext,
    PlaybackActionType.SkipPrevious,
    PlaybackActionType.Sync,
    -> "${fromPosition.clockFormat()} → ${toPosition?.clockFormat()}"

    else -> fromPosition.clockFormat()
  }
}
