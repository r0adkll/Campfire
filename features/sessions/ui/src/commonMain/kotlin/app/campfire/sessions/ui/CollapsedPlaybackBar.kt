package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.core.extensions.progressOver
import app.campfire.core.model.Session
import app.campfire.sessions.ui.composables.Thumbnail
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.time_remaining
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import org.jetbrains.compose.resources.stringResource

private val VerticalOffsetFactor = 24.dp
private val HorizontalOffsetFactor = 8.dp
private val VerticalPaddingFactor = 12.dp
private val HorizontalPaddingFactor = 6.dp
private val HorizontalOffsetPaddingFactor = 8.dp

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
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
  val interactionSource = remember { MutableInteractionSource() }
  var size by remember { mutableStateOf(IntSize.Zero) }

  var dragOffsetX by remember { mutableStateOf(0f) }
  val smoothedOffsetX by animateFloatAsState(dragOffsetX)
  var dragOffsetY by remember { mutableStateOf(0f) }
  val smoothedOffsetY by animateFloatAsState(dragOffsetY)

  val disposingThreshold = with(LocalDensity.current) {
    HorizontalOffsetFactor.roundToPx() * 10
  }
  val isDisposing by remember {
    derivedStateOf {
      if (size == IntSize.Zero) {
        false
      } else {
        abs(dragOffsetX.roundToInt()) > disposingThreshold
      }
    }
  }
  var isDisposed by remember { mutableStateOf(false) }
  LaunchedEffect(isDisposed, smoothedOffsetX) {
    if (isDisposed && (abs(smoothedOffsetX) == size.width.toFloat())) {
      onClearSession()
    }
  }

  val easedOffsetY by remember {
    derivedStateOf {
      val sign = if (smoothedOffsetY >= 0) 1 else -1
      val normalized = (abs(smoothedOffsetY) / 1000f).coerceIn(0f, 1f)
      EaseOutCubic.transform(normalized) * sign
    }
  }

  val easedOffsetX by remember {
    derivedStateOf {
      if (isDisposing) return@derivedStateOf 0f
      val sign = if (smoothedOffsetX >= 0) 1 else -1
      val normalized = (abs(smoothedOffsetX) / 400f).coerceIn(0f, 1f)
      EaseOutCubic.transform(normalized) * sign
    }
  }

  val actualOffsetY = VerticalOffsetFactor * easedOffsetY
  val actualOffsetX = HorizontalOffsetFactor * easedOffsetX
  val actualWithDisposingOffsetX = if (isDisposing) {
    smoothedOffsetX.roundToInt()
  } else {
    with(LocalDensity.current) {
      actualOffsetX.roundToPx()
    }
  }
  val animatedActualOffsetX by animateIntAsState(actualWithDisposingOffsetX)

  val actualVerticalPadding = VerticalPaddingFactor * abs(easedOffsetY)
  val actualHorizontalPadding = HorizontalPaddingFactor * abs(easedOffsetY)
  val horizontalOffsetPadding = HorizontalOffsetPaddingFactor * easedOffsetX

  var isDragging by remember { mutableStateOf(false) }
  val shadowElevation = ShadowElevation * abs(easedOffsetY)
  val tonalElevation = TonalElevation * abs(easedOffsetY)

  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    shape = RoundedCornerShape(12.dp),
    shadowElevation = shadowElevation,
    tonalElevation = tonalElevation,
    modifier = modifier
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = animatedVisibilityScope,
      )
      .draggable2D(
        state = rememberDraggable2DState { delta ->
          dragOffsetX += delta.x
          dragOffsetY += delta.y
        },
        onDragStopped = { velocity ->
          if (
            isDisposing &&
            (abs(dragOffsetX) > (size.width / 3) ||
              abs(velocity.x) > FlingThreshold)
          ) {
            isDisposed = true
            dragOffsetY = 0f
            dragOffsetX = if (dragOffsetX > 0) {
              size.width.toFloat()
            } else {
              -size.width.toFloat()
            }
          } else if (easedOffsetY < -TranslationThreshold || velocity.y < -FlingThreshold) {
            onClick()
            dragOffsetX = 0f
            dragOffsetY = 0f
          }
          isDragging = false
        },
        onDragStarted = {
          isDragging = true
        },
        interactionSource = interactionSource,
      )
      .offset {
        IntOffset(
          x = animatedActualOffsetX,
          y = actualOffsetY.roundToPx(),
        )
      }
      .onSizeChanged { size = it }
      .padding(horizontal = actualHorizontalPadding),
  ) {
    Box(
      modifier = Modifier
        .clickable(
          onClick = onClick,
        )
        .padding(
          vertical = actualVerticalPadding,
          horizontal = (actualHorizontalPadding + horizontalOffsetPadding).coerceAtLeast(0.dp),
        ),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Thumbnail(
          imageUrl = session.libraryItem.media.coverImageUrl,
          contentDescription = session.libraryItem.media.metadata.title,
          modifier = Modifier
            .sharedElement(
              rememberSharedContentState(SharedImage),
              animatedVisibilityScope = animatedVisibilityScope,
            )
            .padding(4.dp),
        )

        Spacer(Modifier.width(16.dp))

        Column(
          modifier = Modifier.weight(1f),
        ) {
          Text(
            text = currentMetadata.title ?: "--",
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier.basicMarquee(),
          )

          Text(
            text = stringResource(Res.string.time_remaining, session.timeRemaining.readoutFormat()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.typography.labelSmall.color.copy(0.7f),
          )
        }

        Spacer(Modifier.width(16.dp))

        IconButton(
          onClick = onRewindClick,
        ) {
          Icon(
            Icons.Rounded.Replay10,
            contentDescription = null,
          )
        }

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
          .alpha(1f - abs(easedOffsetY)),
      )
    }
  }
}


