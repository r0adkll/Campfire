package app.campfire.sessions.ui.playback.collapsed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.extensions.timeAgo
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Sync
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.extensions.fluentIf
import app.campfire.core.extensions.progressOver
import app.campfire.core.model.Session
import app.campfire.sessions.ui.composables.RewindIcon
import app.campfire.sessions.ui.playback.AvailableSync
import app.campfire.sessions.ui.playback.DefaultNonThemedContentColor
import app.campfire.sessions.ui.playback.DefaultNonThemedSheetColor
import app.campfire.sessions.ui.playback.PlayerUiEvent
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.SharedBounds
import app.campfire.sessions.ui.playback.SyncUiEvent
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.collapsed.ActionState.Dispose
import app.campfire.sessions.ui.playback.collapsed.ActionState.None
import app.campfire.sessions.ui.playback.collapsed.ActionState.Open
import app.campfire.sessions.ui.playback.collapsed.composables.PlaybackThumbnail
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_pause
import campfire.features.sessions.ui.generated.resources.action_play
import campfire.features.sessions.ui.generated.resources.action_rewind
import campfire.features.sessions.ui.generated.resources.action_sync_progress
import campfire.features.sessions.ui.generated.resources.clear_session_subtitle
import campfire.features.sessions.ui.generated.resources.clear_session_title
import campfire.features.sessions.ui.generated.resources.sync_available
import campfire.features.sessions.ui.generated.resources.time_remaining
import kotlin.math.abs
import org.jetbrains.compose.resources.stringResource

internal val BaseShadowElevation = 2.dp
internal val ShadowElevation = 4.dp
internal val TonalElevation = 2.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun <T> T.CollapsedPlaybackBar(
  session: Session?,
  playerState: PlayerUiState,
  syncState: SyncUiState,
  onClick: () -> Unit,
  onDispose: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = DefaultNonThemedSheetColor,
  contentColor: Color = DefaultNonThemedContentColor,
) where T : AnimatedVisibilityScope, T : SharedTransitionScope {
  val dragState = remember {
    PlaybackBarDragState(
      onOpen = onClick,
      onDispose = onDispose,
    )
  }

  val shadowElevation = BaseShadowElevation + ShadowElevation * abs(dragState.easedOffsetY)
  val tonalElevation = TonalElevation * abs(dragState.easedOffsetY)

  val surfaceColor by animateColorAsState(
    when {
      dragState.actionState == Dispose -> MaterialTheme.colorScheme.errorContainer
      syncState.availableSync != null -> CampfireTheme.colorScheme.successContainer
      else -> containerColor
    },
  )

  val surfaceContentColor by animateColorAsState(
    when {
      dragState.actionState == Dispose -> MaterialTheme.colorScheme.onErrorContainer
      syncState.availableSync != null -> CampfireTheme.colorScheme.onSuccessContainer
      else -> contentColor
    },
  )

  Surface(
    color = surfaceColor,
    contentColor = surfaceContentColor,
    shape = RoundedCornerShape(12.dp),
    shadowElevation = shadowElevation,
    tonalElevation = tonalElevation,
    onClick = onClick,
    modifier = modifier
      .wrapContentWidth()
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = this@CollapsedPlaybackBar,
      )
      .draggablePlaybackBar(dragState),
    border = when (dragState.actionState) {
      None -> null
      Open -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
      Dispose -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    },
  ) {
    val title = playerState.metadata.title ?: session?.title ?: Session.TITLE_PLACEHOLDER
    val thumbnailUrl = playerState.metadata.artworkUri ?: session?.libraryItem?.media?.coverImageUrl
    val thumbnailContentDescription = session?.libraryItem?.media?.metadata?.title
    val timeRemaining = session?.timeRemaining
      ?.div(playerState.speed.toDouble())
      ?.readoutFormat() ?: "--"

    CollapsedPlaybackBarContent(
      dragState = dragState,
      title = title,
      thumbnailUrl = thumbnailUrl,
      thumbnailContentDescription = thumbnailContentDescription,
      state = playerState.state,
      progress = {
        playerState.time progressOver playerState.duration
      },
      timeRemaining = timeRemaining,
      isAccelerated = playerState.speed != 1f,
      runningTimer = playerState.timer,
      availableSync = syncState.availableSync,
      onSync = {
        syncState.eventSink(SyncUiEvent.Sync)
      },
      onPlayPauseClick = {
        playerState.eventSink(PlayerUiEvent.PlayPauseClick)
      },
      onRewindClick = {
        playerState.eventSink(PlayerUiEvent.RewindClick)
      },
      sharedTransitionScope = this@CollapsedPlaybackBar,
      animatedVisibilityScope = this@CollapsedPlaybackBar,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedPlaybackBarContent(
  dragState: PlaybackBarDragState,
  title: String,
  thumbnailUrl: String?,
  thumbnailContentDescription: String?,
  state: AudioPlayer.State,
  progress: () -> Float,
  timeRemaining: String,
  isAccelerated: Boolean,
  runningTimer: RunningTimer?,
  availableSync: AvailableSync?,
  onSync: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .padding(dragState.contentPadding),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      PlaybackThumbnail(
        thumbnailUrl = thumbnailUrl,
        thumbnailContentDescription = thumbnailContentDescription,
        animatedVisibilityScope = animatedVisibilityScope,
        runningTimer = runningTimer,
        availableSync = availableSync,
        dragState = dragState,
      )

      Spacer(Modifier.width(12.dp))

      Column(
        modifier = Modifier.weight(1f),
      ) {
        val playbackBarTitle = when {
          dragState.actionState == Dispose -> stringResource(Res.string.clear_session_title)
          availableSync != null -> stringResource(Res.string.sync_available, availableSync.syncTimeInMillis.timeAgo)
          else -> title
        }

        Text(
          text = playbackBarTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          fontFamily = PaytoneOneFontFamily,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
        )

        val subtitle = when {
          dragState.actionState == Dispose -> stringResource(Res.string.clear_session_subtitle)
          availableSync != null -> "Update to ${availableSync.targetTime.readoutFormat()}"
          else -> stringResource(Res.string.time_remaining, timeRemaining)
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
        ) {
          AnimatedVisibility(
            visible = isAccelerated &&
              dragState.actionState != Dispose &&
              availableSync == null,
          ) {
            Icon(
              Icons.Rounded.KeyboardDoubleArrowRight,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.secondary,
            )
          }

          Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated) {
              copy(
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
              )
            },
            modifier = Modifier.alpha(0.75f),
          )
        }
      }

      Spacer(Modifier.width(16.dp))

      AnimatedVisibility(
        visible = dragState.actionState != Dispose && availableSync == null,
      ) {
        val rewindLabel = stringResource(Res.string.action_rewind)
        IconButtonTooltip(text = rewindLabel) {
          IconButton(
            onClick = onRewindClick,
          ) {
            RewindIcon()
          }
        }
      }

      AnimatedVisibility(
        visible = dragState.actionState != Dispose && availableSync != null,
      ) {
        val syncLabel = stringResource(Res.string.action_sync_progress)
        IconButtonTooltip(text = syncLabel) {
          IconButton(
            onClick = onSync,
          ) {
            Icon(
              CampfireIcons.Rounded.Sync,
              contentDescription = syncLabel,
            )
          }
        }
      }

      AnimatedVisibility(
        visible = dragState.actionState != Dispose,
      ) {
        Box {
          val isPlaying = state == AudioPlayer.State.Playing
          val playPauseLabel = stringResource(
            if (isPlaying) Res.string.action_pause else Res.string.action_play,
          )
          IconButtonTooltip(text = playPauseLabel) {
            IconButton(
              onClick = onPlayPauseClick,
            ) {
              Icon(
                if (isPlaying) {
                  Icons.Rounded.Pause
                } else {
                  Icons.Rounded.PlayArrow
                },
                contentDescription = playPauseLabel,
              )
            }
          }

          if (state == AudioPlayer.State.Buffering) {
            CircularProgressIndicator(
              modifier = Modifier
                .size(32.dp)
                .align(Alignment.Center),
              strokeWidth = 2.dp,
            )
          }
        }
      }

      Spacer(Modifier.width(16.dp))
    }

    LinearProgressIndicator(
      progress = progress,
      modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(
          horizontal = 12.dp,
        )
        .height(2.dp)
        .fillMaxWidth()
        .alpha(1f - abs(dragState.easedOffsetY)),
    )
  }
}

private val VerticalOffsetFactor = 24.dp
private val HorizontalOffsetFactor = 8.dp
private val VerticalPaddingFactor = 12.dp
private val HorizontalPaddingFactor = 6.dp
private val HorizontalOffsetPaddingFactor = 8.dp
private const val ActionStateThresholdIncrement = 1f / 10f
private const val OpenVelocityThreshold = -3900 // px/s

internal enum class ActionState {
  None,
  Open,
  Dispose,
}

@Stable
internal class PlaybackBarDragState(
  private val onOpen: () -> Unit,
  private val onDispose: () -> Unit,
) {
  val interactionSource = MutableInteractionSource()
  var isDragging by mutableStateOf(false)

  var parentSize by mutableStateOf(IntSize.Zero)

  var rawOffsetX by mutableStateOf(0f)
  var rawOffsetY by mutableStateOf(0f)

  val easedOffsetX by derivedStateOf {
    val sign = if (rawOffsetX >= 0) 1 else -1
    val normalized = (abs(rawOffsetX) / 400f).coerceIn(0f, 1f)
    EaseOutCubic.transform(normalized) * sign
  }

  val easedOffsetY by derivedStateOf {
    val sign = if (rawOffsetY >= 0) 1 else -1
    val normalized = (abs(rawOffsetY) / 1000f).coerceIn(0f, 1f)
    EaseOutCubic.transform(normalized) * sign
  }

  // FIXME: There is probably a better way to organize this logic to be easier to grok
  val actionState by derivedStateOf {
    val y = abs(rawOffsetY)
    when {
      // Enter 'Dispose' mode when drag-y is > 3/10th of the parent height
      y > (parentSize.height * (ActionStateThresholdIncrement * 3f)) -> Dispose
      // Enter 'Open' mode when drag-y is > 1/10th of the parent height
      y > (parentSize.height * (ActionStateThresholdIncrement)) -> Open
      else -> None
    }
  }

  val actualOffsetX by derivedStateOf {
    HorizontalOffsetFactor * easedOffsetX
  }

  val actualOffsetY by derivedStateOf {
    VerticalOffsetFactor * easedOffsetY
  }

  val contentPadding: PaddingValues
    get() {
      val actualVerticalPadding = VerticalPaddingFactor * abs(easedOffsetY)
      val actualHorizontalPadding = HorizontalPaddingFactor * abs(easedOffsetY)
      val horizontalOffsetPadding = HorizontalOffsetPaddingFactor * easedOffsetX
      return PaddingValues(
        vertical = actualVerticalPadding,
        horizontal = (actualHorizontalPadding + horizontalOffsetPadding).coerceAtLeast(0.dp),
      )
    }

  internal fun onDragStarted(startedPosition: Offset) {
    isDragging = true
  }

  internal fun onDragStopped(velocity: Velocity) {
    // Check if the velocity is over the opening threshold. If so then
    // we can ignore the positional action state and just call the open
    if (velocity.y <= OpenVelocityThreshold) {
      onOpen()
      return
    }

    // Check the positional action state to determine which action to take
    when (actionState) {
      Open -> onOpen()
      Dispose -> onDispose()
      None -> Unit
    }

    // Reset the state
    isDragging = false
    rawOffsetX = 0f
    rawOffsetY = 0f
  }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Modifier.draggablePlaybackBar(
  state: PlaybackBarDragState,
): Modifier = with(LocalDensity.current) {
  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(state.actionState) {
    if (state.actionState != None) {
      hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }

  val additionalStateHorizontalPadding by animateDpAsState(
    when (state.actionState) {
      None -> 0.dp
      Open -> 8.dp
      Dispose -> 16.dp
    },
  )

  val additionalStateVerticalPadding by animateDpAsState(
    when (state.actionState) {
      None -> 0.dp
      Open -> 4.dp
      Dispose -> 48.dp
    },
  )

  val actualHorizontalPadding = HorizontalPaddingFactor * abs(state.easedOffsetY)

  val animatedOffsetX by animateFloatAsState(state.easedOffsetX)
  val animatedOffsetY by animateFloatAsState(state.easedOffsetY)

  val actualX by derivedStateOf {
    if (!state.isDragging) {
      animatedOffsetX.fastRoundToInt()
    } else {
      state.actualOffsetX.roundToPx()
    }
  }

  val actualY by derivedStateOf {
    if (!state.isDragging) {
      animatedOffsetY.fastRoundToInt()
    } else {
      state.actualOffsetY.roundToPx()
    }
  }

  return this@draggablePlaybackBar
    .draggable2D(
      state = rememberDraggable2DState { delta ->
        state.rawOffsetX += delta.x
        state.rawOffsetY += delta.y
      },
      onDragStopped = state::onDragStopped,
      onDragStarted = state::onDragStarted,
      interactionSource = state.interactionSource,
    )
    .onGloballyPositioned {
      val rootCoordinates = it.findRootCoordinates()
      state.parentSize = rootCoordinates.size
    }
    .offset {
      IntOffset(
        x = actualX,
        y = actualY,
      )
    }
    .padding(
      horizontal = actualHorizontalPadding + additionalStateHorizontalPadding,
      vertical = additionalStateVerticalPadding,
    )
}
