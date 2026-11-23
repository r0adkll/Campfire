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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRepository
import app.campfire.audioplayer.loadedPlayerFor
import app.campfire.audioplayer.observeHasLoaded
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.extensions.clockFormat
import app.campfire.common.compose.extensions.relativeDayLabel
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.ArrowForward
import app.campfire.common.compose.icons.rounded.CheckCircle
import app.campfire.common.compose.icons.rounded.DeleteSweep
import app.campfire.common.compose.icons.rounded.FastForward
import app.campfire.common.compose.icons.rounded.FastRewind
import app.campfire.common.compose.icons.rounded.HistoryEdu
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.SkipNext
import app.campfire.common.compose.icons.rounded.SkipPrevious
import app.campfire.common.compose.icons.rounded.Sync
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.sheets.AdaptiveSheetOverlay
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.sessions.api.PlaybackHistoryUi
import app.campfire.sessions.ui.sheets.SessionSheetLayout
import app.campfire.sessions.ui.sheets.rememberSessionSheetTitleState
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_clear_history
import campfire.features.sessions.ui.generated.resources.history_bottomsheet_title
import campfire.features.sessions.ui.generated.resources.history_context_finished
import campfire.features.sessions.ui.generated.resources.history_context_pause
import campfire.features.sessions.ui.generated.resources.history_context_play
import campfire.features.sessions.ui.generated.resources.history_context_seek
import campfire.features.sessions.ui.generated.resources.history_context_seek_backward
import campfire.features.sessions.ui.generated.resources.history_context_seek_forward
import campfire.features.sessions.ui.generated.resources.history_context_skip_next
import campfire.features.sessions.ui.generated.resources.history_context_skip_previous
import campfire.features.sessions.ui.generated.resources.history_context_sync
import campfire.features.sessions.ui.generated.resources.history_empty_message
import campfire.features.sessions.ui.generated.resources.history_jump_hint
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface PlaybackHistoryBottomSheetComponent {
  val playbackHistoryRepository: PlaybackHistoryRepository
  val audioPlayerHolder: AudioPlayerHolder
}

/** Whose history the sheet shows: an item's, or with an [episodeId] just that episode's. */
data class PlaybackHistoryInput(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
)

sealed interface PlaybackHistoryResult {
  data object None : PlaybackHistoryResult
  data class Selected(val action: PlaybackAction) : PlaybackHistoryResult
}

@ContributesBinding(UserScope::class)
@Inject
class OverlayPlaybackHistoryUi : PlaybackHistoryUi {

  override suspend fun showPlaybackHistory(
    overlayHost: OverlayHost,
    libraryItemId: LibraryItemId,
  ): Duration? {
    val result = overlayHost.showPlaybackHistoryBottomSheet(PlaybackHistoryInput(libraryItemId))
    return (result as? PlaybackHistoryResult.Selected)?.action?.jumpPosition
  }
}

/** Where jumping to this event lands: where it left playback */
internal val PlaybackAction.jumpPosition: Duration
  get() = toPosition ?: fromPosition

suspend fun OverlayHost.showPlaybackHistoryBottomSheet(
  input: PlaybackHistoryInput,
): PlaybackHistoryResult {
  return show(
    AdaptiveSheetOverlay<PlaybackHistoryInput, PlaybackHistoryResult>(
      model = input,
      onDismiss = { PlaybackHistoryResult.None },
      contentDrawsDragHandle = true,
    ) { model, overlayNavigator ->
      PlaybackHistoryBottomSheet(
        input = model,
        navigator = overlayNavigator,
        modifier = Modifier.navigationBarsPadding(),
      )
    },
  )
}

/**
 * State/DI layer for the playback history sheet: observes the history, and whether the player has
 * its item loaded, so [PlaybackHistorySheet] stays pure and previewable.
 */
@Composable
internal fun PlaybackHistoryBottomSheet(
  input: PlaybackHistoryInput,
  navigator: OverlayNavigator<PlaybackHistoryResult>,
  modifier: Modifier = Modifier,
  component: PlaybackHistoryBottomSheetComponent = rememberComponent(),
) {
  val scope = rememberCoroutineScope()

  val actions by remember(input) {
    component.playbackHistoryRepository.observe(input.libraryItemId, input.episodeId)
  }.collectAsState(emptyList())

  // Live, so the sheet opens up or goes read-only as playback starts or stops while it's open
  val initiallyInteractive = remember(input) {
    component.audioPlayerHolder.loadedPlayerFor(input.libraryItemId, input.episodeId) != null
  }
  val interactive by remember(input) {
    component.audioPlayerHolder.observeHasLoaded(input.libraryItemId, input.episodeId)
  }.collectAsState(initiallyInteractive)

  PlaybackHistorySheet(
    actions = actions,
    interactive = interactive,
    onActionClick = { action -> navigator.finish(PlaybackHistoryResult.Selected(action)) },
    onClearClick = {
      scope.launch {
        component.playbackHistoryRepository.clear(input.libraryItemId, input.episodeId)
      }
    },
    modifier = modifier,
  )
}

/**
 * Playback [actions], newest first and grouped by day.
 *
 * @param interactive whether an event can be picked to jump to and the history cleared; when false,
 * as for an item the player hasn't loaded, the history is only for reading
 */
@Composable
internal fun PlaybackHistorySheet(
  actions: List<PlaybackAction>,
  interactive: Boolean,
  onActionClick: (PlaybackAction) -> Unit,
  onClearClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val sessionSheetState = rememberSessionSheetTitleState()

  SessionSheetLayout(
    modifier = modifier,
    state = sessionSheetState,
    title = {
      Text(stringResource(Res.string.history_bottomsheet_title))
    },
    subtitle = {
      // Only events that can be jumped to are worth telling to tap
      if (interactive) {
        Text(
          text = stringResource(Res.string.history_jump_hint),
          style = MaterialTheme.typography.labelMedium,
          fontStyle = FontStyle.Italic,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.tertiary,
        )
      }
    },
    trailingContent = {
      // History only to be read isn't cleared from here either
      AnimatedVisibility(
        visible = actions.isNotEmpty() && interactive,
        modifier = Modifier.align(Alignment.CenterEnd),
      ) {
        val clearHistoryLabel = stringResource(Res.string.action_clear_history)
        IconButtonTooltip(text = clearHistoryLabel) {
          IconButton(onClick = onClearClick) {
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
          CampfireIcons.Rounded.HistoryEdu,
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
              // Jumping to a Finished entry would seek to the very end and finish the book again
              modifier = if (!interactive || action.type == PlaybackActionType.Finished) {
                Modifier
              } else {
                Modifier.clickable { onActionClick(action) }
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
  PlaybackActionType.Play -> CampfireIcons.Rounded.PlayArrow
  PlaybackActionType.Pause -> CampfireIcons.Rounded.Pause
  PlaybackActionType.Seek -> CampfireIcons.Rounded.ArrowForward
  PlaybackActionType.SkipNext -> CampfireIcons.Rounded.SkipNext
  PlaybackActionType.SkipPrevious -> CampfireIcons.Rounded.SkipPrevious
  PlaybackActionType.SeekForward -> CampfireIcons.Rounded.FastForward
  PlaybackActionType.SeekBackward -> CampfireIcons.Rounded.FastRewind
  PlaybackActionType.Sync -> CampfireIcons.Rounded.Sync
  PlaybackActionType.Finished -> CampfireIcons.Rounded.CheckCircle
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
  PlaybackActionType.Finished -> MaterialTheme.colorScheme.primary
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
    PlaybackActionType.Finished -> stringResource(Res.string.history_context_finished)
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

@Preview
@Composable
private fun PlaybackHistorySheetPreview() {
  CampfireTheme {
    PlaybackHistorySheet(
      actions = PreviewActions,
      interactive = true,
      onActionClick = {},
      onClearClick = {},
    )
  }
}

@Preview
@Composable
private fun PlaybackHistorySheetReadOnlyPreview() {
  CampfireTheme {
    PlaybackHistorySheet(
      actions = PreviewActions,
      interactive = false,
      onActionClick = {},
      onClearClick = {},
    )
  }
}

@Preview
@Composable
private fun PlaybackHistorySheetEmptyPreview() {
  CampfireTheme {
    PlaybackHistorySheet(
      actions = emptyList(),
      interactive = true,
      onActionClick = {},
      onClearClick = {},
    )
  }
}

private val PreviewActions = listOf(
  previewAction(4, PlaybackActionType.Pause, LocalDateTime(2026, 1, 2, 21, 40), from = 72.minutes),
  previewAction(3, PlaybackActionType.SeekBackward, LocalDateTime(2026, 1, 2, 21, 12), 52.minutes, 51.minutes),
  previewAction(2, PlaybackActionType.Play, LocalDateTime(2026, 1, 2, 20, 30), from = 10.minutes),
  previewAction(1, PlaybackActionType.Finished, LocalDateTime(2026, 1, 1, 22, 5), from = 600.minutes),
)

private fun previewAction(
  id: Long,
  type: PlaybackActionType,
  timestamp: LocalDateTime,
  from: Duration,
  to: Duration? = null,
) = PlaybackAction(
  id = id,
  libraryItemId = "item",
  userId = "user",
  type = type,
  timestamp = timestamp,
  fromPosition = from,
  toPosition = to,
)
