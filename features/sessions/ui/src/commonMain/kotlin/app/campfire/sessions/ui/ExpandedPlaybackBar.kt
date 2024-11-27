package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.CoverImageSize
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import app.campfire.sessions.ui.chapters.ChapterResult
import app.campfire.sessions.ui.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.composables.SpeedPickerButton
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.time.Duration
import kotlinx.coroutines.launch

private val ExpandedVerticalOffsetFactor = 56.dp
private val ExpandedHorizontalOffsetFactor = 4.dp
private val ExpandedCornerRadiusFactor = 24.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ExpandedPlaybackBar(
  state: AudioPlayer.State,
  playbackSpeed: Float,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,

  session: Session,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeek: (Float) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onChapterSelected: (Chapter) -> Unit,

  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  val windowSizeClass = LocalWindowSizeClass.current
  val overlayHost = LocalOverlayHost.current
  val scope = rememberCoroutineScope()

  // Motion Stuff
  var dragOffset by remember { mutableStateOf(0f) }
  val smoothedOffset by animateFloatAsState(dragOffset)
  val easedOffset by remember {
    derivedStateOf {
      val normalized = (smoothedOffset.coerceAtLeast(0f) / 1000f).coerceIn(0f, 1f)
      EaseOutCubic.transform(normalized)
    }
  }
  val actualVerticalOffset = ExpandedVerticalOffsetFactor * easedOffset
  val actualHorizontalOffset = ExpandedHorizontalOffsetFactor * easedOffset
  val actualCornerRadius = if (windowSizeClass.isSupportingPaneEnabled) {
    ExpandedCornerRadiusFactor
  } else {
    ExpandedCornerRadiusFactor * easedOffset
  }

  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = modifier
      .fillMaxSize()
      .sharedBounds(
        rememberSharedContentState(SharedBounds),
        animatedVisibilityScope = animatedVisibilityScope,
      )
      .draggable(
        state = rememberDraggableState { delta ->
          dragOffset += delta
        },
        orientation = Orientation.Vertical,
        onDragStopped = { velocity ->
          if (easedOffset > TranslationThreshold || velocity > FlingThreshold) onClose()
          dragOffset = 0f
        },
      )
      .fluentIf(windowSizeClass.isSupportingPaneEnabled) {
        padding(top = 32.dp)
      }
      .padding(
        top = actualVerticalOffset,
        start = actualHorizontalOffset,
        end = actualHorizontalOffset,
      ),
    shape = RoundedCornerShape(actualCornerRadius),
    shadowElevation = ShadowElevation,
    tonalElevation = TonalElevation,
  ) {
    Column {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(
            onClick = onClose,
          ) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null)
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.secondaryContainer,
          navigationIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
          actionIconContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        windowInsets = if (windowSizeClass.isSupportingPaneEnabled) {
          WindowInsets(0.dp)
        } else {
          TopAppBarDefaults.windowInsets
        },
      )

      Column(
        Modifier.weight(1f),
      ) {
        Spacer(Modifier.height(16.dp))

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          CoverImage(
            imageUrl = session.libraryItem.media.coverImageUrl,
            contentDescription = session.libraryItem.media.metadata.title,
            size = if (windowSizeClass.isSupportingPaneEnabled) {
              188.dp
            } else {
              CoverImageSize
            },
            modifier = Modifier
              .sharedElement(
                rememberSharedContentState(SharedImage),
                animatedVisibilityScope = animatedVisibilityScope,
              ),
          )

          Spacer(Modifier.height(16.dp))

          Text(
            text = currentMetadata.title ?: "--",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .padding(horizontal = 24.dp),
          )

          Text(
            text = session.libraryItem.media.metadata.title ?: "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .padding(horizontal = 24.dp)
              .alpha(50f),
          )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val isDragged by interactionSource.collectIsDraggedAsState()
        val isInteracting = isPressed || isDragged

        fun calculateProgress(): Float = if (currentDuration.inWholeMilliseconds == 0L) {
          0f
        } else {
          currentTime.inWholeMilliseconds.toFloat() /
            currentDuration.inWholeMilliseconds.toFloat()
        }

        var sliderValue by remember { mutableStateOf(calculateProgress()) }
        LaunchedEffect(isInteracting, state, currentTime, currentDuration) {
          if (!isInteracting && state == AudioPlayer.State.Playing) {
            sliderValue = calculateProgress()
          }
        }

        Slider(
          value = sliderValue,
          onValueChange = { sliderValue = it },
          onValueChangeFinished = {
            onSeek(sliderValue)
          },
          interactionSource = interactionSource,
          colors = SliderDefaults.colors(
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        )
        Row(
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        ) {
          val currentTimeLabel = if (isInteracting) {
            currentDuration.times(sliderValue.toDouble()).readoutFormat()
          } else {
            currentTime.readoutFormat()
          }

          Text(
            text = currentTimeLabel,
            style = MaterialTheme.typography.labelSmall,
          )

          Spacer(Modifier.weight(1f))

          val currentRemainingDuration = currentDuration - currentTime
          Text(
            text = currentRemainingDuration.readoutFormat(),
            style = MaterialTheme.typography.labelSmall,
          )
        }

        Spacer(Modifier.height(32.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
          IconButton(
            onClick = onSkipPreviousClick,
          ) {
            Icon(
              Icons.Rounded.SkipPrevious,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          IconButton(
            onClick = onRewindClick,
          ) {
            Icon(
              Icons.Rounded.Replay10,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          val isPlayPauseEnabled = state != AudioPlayer.State.Disabled &&
            state != AudioPlayer.State.Buffering &&
            !isInteracting

          val elevation by animateDpAsState(
            targetValue = if (isPlayPauseEnabled) {
              6.dp
            } else {
              1.dp
            }
          )

          Surface(
            shape = CircleShape,
            modifier = Modifier.size(90.dp),
            shadowElevation = elevation,
            onClick = onPlayPauseClick,
            enabled = isPlayPauseEnabled,
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              if (state != AudioPlayer.State.Buffering) {
                Icon(
                  if (isInteracting) {
                    Icons.Rounded.EditAudio
                  } else if (state == AudioPlayer.State.Playing) {
                    Icons.Rounded.Pause
                  } else {
                    Icons.Rounded.PlayArrow
                  },
                  modifier = Modifier
                    .size(48.dp)
                    .alpha(if (isPlayPauseEnabled) 1f else 0.5f),
                  contentDescription = null,
                )
              } else {
                CircularProgressIndicator(
                  modifier = Modifier.size(48.dp),
                  strokeWidth = 4.dp,
                )
              }
            }
          }

          IconButton(
            onClick = onForwardClick,
          ) {
            Icon(
              Icons.Rounded.Forward10,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }

          IconButton(
            onClick = onSkipNextClick,
          ) {
            Icon(
              Icons.Rounded.SkipNext,
              modifier = Modifier.size(48.dp),
              contentDescription = null,
            )
          }
        }
      }

      Spacer(Modifier.height(24.dp))

      ActionRow(
        playbackSpeed = playbackSpeed,
        onSpeedChange = onSpeedChange,
        onChapterListClick = {
          scope.launch {
            val result = overlayHost.showChapterBottomSheet(session.libraryItem.media.chapters)
            if (result is ChapterResult.Selected) {
              onChapterSelected(result.chapter)
            }
          }
        }
      )

      Spacer(Modifier.navigationBarsPadding())
    }
  }
}

@Composable
private fun ActionRow(
  playbackSpeed: Float,
  onSpeedChange: (Float) -> Unit,
  onChapterListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    IconButton(
      onClick = {},
    ) {
      Icon(Icons.Rounded.BookmarkAdd, contentDescription = null)
    }

    SpeedPickerButton(
      speed = playbackSpeed,
      speeds = listOf(3f, 2f, 1.5f, 1f, 0.5f),
      onSpeedPicked = onSpeedChange,
    )

    IconButton(
      onClick = {},
    ) {
      Icon(Icons.Rounded.Timer, contentDescription = null)
    }
    IconButton(
      onClick = onChapterListClick,
    ) {
      Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
    }
  }
}
