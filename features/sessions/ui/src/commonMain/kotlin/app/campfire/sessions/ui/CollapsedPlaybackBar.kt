package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.core.extensions.progressOver
import app.campfire.core.model.Session
import app.campfire.sessions.ui.ActionState.Dispose
import app.campfire.sessions.ui.ActionState.None
import app.campfire.sessions.ui.ActionState.Open
import app.campfire.sessions.ui.composables.Thumbnail
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.time_remaining
import kotlin.math.abs
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun CollapsedPlaybackBar(
  session: Session,
  state: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,
  onClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onClearSession: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  val dragState = remember {
    PlaybackBarDragState(
      onOpen = onClick,
      onDispose = onClearSession,
    )
  }

  val shadowElevation = ShadowElevation * abs(dragState.easedOffsetY)
  val tonalElevation = TonalElevation * abs(dragState.easedOffsetY)

  val surfaceColor by animateColorAsState(
    when (dragState.actionState) {
      Dispose -> MaterialTheme.colorScheme.errorContainer
      Open,
      None,
      -> MaterialTheme.colorScheme.secondaryContainer
    },
  )

  Surface(
    color = surfaceColor,
    shape = RoundedCornerShape(12.dp),
    shadowElevation = shadowElevation,
    tonalElevation = tonalElevation,
    modifier = modifier
      .wrapContentWidth()
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = animatedVisibilityScope,
      )
      .draggablePlaybackBar(dragState),
    border = when (dragState.actionState) {
      None -> null
      Open -> BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
      Dispose -> BorderStroke(2.dp, MaterialTheme.colorScheme.error)
    },
  ) {
    CollapsedPlaybackBarContent(
      dragState = dragState,
      session = session,
      state = state,
      currentTime = currentTime,
      currentDuration = currentDuration,
      currentMetadata = currentMetadata,
      onClick = onClick,
      onPlayPauseClick = onPlayPauseClick,
      onRewindClick = onRewindClick,
      sharedTransitionScope = sharedTransitionScope,
      animatedVisibilityScope = animatedVisibilityScope,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedPlaybackBarContent(
  dragState: PlaybackBarDragState,
  session: Session,
  state: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,
  onClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  Box(
    modifier = modifier
      .clickable(
        onClick = onClick,
      )
      .fillMaxWidth()
      .padding(dragState.contentPadding),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Box(
        modifier = Modifier.padding(4.dp),
        contentAlignment = Alignment.Center,
      ) {
        val mediaUrl = currentMetadata.artworkUri
          ?: session.libraryItem.media.coverImageUrl
        Thumbnail(
          imageUrl = mediaUrl,
          contentDescription = session.libraryItem.media.metadata.title,
          modifier = Modifier
            .sharedElement(
              rememberSharedContentState(SharedImage),
              animatedVisibilityScope = animatedVisibilityScope,
            ),
        )

        androidx.compose.animation.AnimatedVisibility(
          visible = dragState.actionState == Dispose,
          enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .background(
                color = MaterialTheme.colorScheme.error.copy(0.6f),
                shape = RoundedCornerShape(8.dp),
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              Icons.Rounded.DeleteSweep,
              contentDescription = null,
              tint = Color.White,
            )
          }
        }
      }

      Spacer(Modifier.width(16.dp))

      Column(
        modifier = Modifier.weight(1f),
      ) {
        val title = when (dragState.actionState) {
          Dispose -> "Clear session"
          else -> currentMetadata.title ?: "--"
        }

        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
          fontFamily = PaytoneOneFontFamily,
          maxLines = 1,
          modifier = Modifier.basicMarquee(),
        )

        val subtitle = when (dragState.actionState) {
          Dispose -> "Stop playback?"
          else -> stringResource(Res.string.time_remaining, session.timeRemaining.readoutFormat())
        }

        Text(
          text = subtitle,
          style = MaterialTheme.typography.labelSmall,
          modifier = Modifier.alpha(0.7f),
        )
      }

      Spacer(Modifier.width(16.dp))

      androidx.compose.animation.AnimatedVisibility(
        visible = dragState.actionState != Dispose,
      ) {
        IconButton(
          onClick = onRewindClick,
        ) {
          Icon(
            Icons.Rounded.Replay10,
            contentDescription = null,
          )
        }
      }

      androidx.compose.animation.AnimatedVisibility(
        visible = dragState.actionState != Dispose,
      ) {
        Box {
          IconButton(
            enabled = state != AudioPlayer.State.Disabled,
            onClick = onPlayPauseClick,
          ) {
            Icon(
              if (state == AudioPlayer.State.Playing) {
                Icons.Rounded.Pause
              } else {
                Icons.Rounded.PlayArrow
              },
              contentDescription = null,
            )
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
      progress = {
        currentTime progressOver currentDuration
      },
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
