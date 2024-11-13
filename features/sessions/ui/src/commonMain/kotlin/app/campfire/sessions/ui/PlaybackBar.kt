package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import app.campfire.audioplayer.AudioPlayer
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.CoverImageSize
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Session
import app.campfire.sessions.ui.PlaybackBarState.Collapsed
import app.campfire.sessions.ui.PlaybackBarState.Expanded
import app.campfire.sessions.ui.PlaybackBarState.Hidden
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.buffering
import campfire.features.sessions.ui.generated.resources.time_remaining
import coil3.compose.rememberAsyncImagePainter
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.emptyFlow
import org.jetbrains.compose.resources.stringResource

enum class PlaybackBarState {
  Hidden,
  Collapsed,
  Expanded,
}

private const val FlingThreshold = 4000f
private const val TranslationThreshold = 0.75f

private val ShadowElevation = 4.dp
private val TonalElevation = 2.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaybackBar(
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  SessionHostLayout { currentSession, audioPlayer, clearSession ->

    val currentDuration by remember(audioPlayer) {
      audioPlayer?.currentDuration ?: emptyFlow()
    }.collectAsState(0.seconds)

    val playerState by remember(audioPlayer) {
      audioPlayer?.state ?: emptyFlow()
    }.collectAsState(AudioPlayer.State.Disabled)

    val playbackSpeed by remember(audioPlayer) {
      audioPlayer?.playbackSpeed ?: emptyFlow()
    }.collectAsState(1f)

    SharedTransitionLayout(
      modifier = modifier,
    ) {
      AnimatedContent(
        targetState = when {
          currentSession == null -> Hidden
          expanded -> Expanded
          else -> Collapsed
        },
        transitionSpec = {
          when {
            (initialState == Hidden && targetState == Collapsed) ||
              (initialState == Collapsed && targetState == Hidden)
              -> slideInVertically { it } togetherWith slideOutVertically { it }

            else -> scaleIn() togetherWith scaleOut()
          }
        },
      ) { state ->
        when (state) {
          Hidden -> Unit
          Collapsed -> {
            if (currentSession == null) return@AnimatedContent
            PlaybackBar(
              session = currentSession,
              state = playerState,
              onClick = { onExpansionChange(!expanded) },
              onPlayPauseClick = {
                audioPlayer?.playPause()
              },
              onRewindClick = {
                audioPlayer?.seekBackward()
              },
              onClearSession = clearSession,
              sharedTransitionScope = this@SharedTransitionLayout,
              animatedVisibilityScope = this,
              modifier = Modifier.padding(8.dp),
            )
          }

          Expanded -> {
            ExpandedPlaybackBar(
                session = currentSession!!,
                state = playerState,
                playbackSpeed = playbackSpeed,
                durationOverride = currentDuration,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
                onPlayPauseClick = { audioPlayer?.playPause() },
                onRewindClick = { audioPlayer?.seekBackward() },
                onForwardClick = { audioPlayer?.seekForward() },
                onSkipPreviousClick = { audioPlayer?.skipToPrevious() },
                onSkipNextClick = { audioPlayer?.skipToNext() },
                onClose = { onExpansionChange(false) },
                onSeek = { progress ->
                    audioPlayer?.seekTo(progress)
                },
                onSpeedChange = { speed ->
                    audioPlayer?.setPlaybackSpeed(speed)
                },
            )
          }
        }
      }
    }
  }
}

private val ExpandedVerticalOffsetFactor = 56.dp
private val ExpandedHorizontalOffsetFactor = 4.dp
private val ExpandedCornerRadiusFactor = 24.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedPlaybackBar(
  state: AudioPlayer.State,
  playbackSpeed: Float,
  session: Session,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeek: (Float) -> Unit,
  onSpeedChange: (Float) -> Unit,
  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  durationOverride: Duration? = null,
) = with(sharedTransitionScope) {
  val windowSizeClass = LocalWindowSizeClass.current

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
            text = session.title,
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

        var sliderValue by remember { mutableStateOf(session.chapterProgress) }
        LaunchedEffect(session, isInteracting) {
          if (!isInteracting && state == AudioPlayer.State.Playing) {
            sliderValue = session.chapterProgress
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
            durationOverride?.times(sliderValue.toDouble())?.readoutFormat()
          } else if (state == AudioPlayer.State.Buffering) {
            stringResource(Res.string.buffering)
          } else {
            null
          }

          Text(
            text = currentTimeLabel ?: session.currentTime.readoutFormat(),
            style = MaterialTheme.typography.labelSmall,
          )

          Spacer(Modifier.weight(1f))

          Text(
            text = (durationOverride ?: session.duration).readoutFormat(),
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

          Surface(
            shape = CircleShape,
            modifier = Modifier.size(90.dp),
            shadowElevation = 4.dp,
            onClick = onPlayPauseClick,
            enabled = state != AudioPlayer.State.Disabled && state != AudioPlayer.State.Buffering,
          ) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center,
            ) {
              if (state != AudioPlayer.State.Buffering) {
                Icon(
                  if (state == AudioPlayer.State.Playing) {
                    Icons.Rounded.Pause
                  } else {
                    Icons.Rounded.PlayArrow
                  },
                  modifier = Modifier.size(48.dp),
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

      Row(
        modifier = Modifier
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
          onSpeedPicked = onSpeedChange,
        )

        IconButton(
          onClick = {},
        ) {
          Icon(Icons.Rounded.Timer, contentDescription = null)
        }
        IconButton(
          onClick = {},
        ) {
          Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
        }
      }

      Spacer(Modifier.navigationBarsPadding())
    }
  }
}

@Composable
private fun SpeedPickerButton(
  speed: (Float),
  onSpeedPicked: (Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(modifier) {
    var isExpanded by remember { mutableStateOf(false) }
    IconButton(
      onClick = { isExpanded = true },
    ) {
      Icon(Icons.Rounded.Speed, contentDescription = null)
    }

    DropdownMenu(
      expanded = isExpanded,
      onDismissRequest = { isExpanded = false },
      offset = DpOffset((-4).dp, 64.dp)
    ) {
      SpeedMenuItem(
        speed = 2f,
        isSelected = speed == 2f,
        onClick = {
          isExpanded = false
          onSpeedPicked(2f)
        },
      )
      SpeedMenuItem(
        speed = 1.5f,
        isSelected = speed == 1.5f,
        onClick = {
          isExpanded = false
          onSpeedPicked(1.5f)
        },
      )
      SpeedMenuItem(
        speed = 1f,
        isSelected = speed == 1f,
        onClick = {
          isExpanded = false
          onSpeedPicked(1f)
        },
      )
      SpeedMenuItem(
        speed = 0.5f,
        isSelected = speed == 0.5f,
        onClick = {
          isExpanded = false
          onSpeedPicked(0.5f)
        },
      )
    }
  }
}

@Composable
private fun SpeedMenuItem(
  speed: Float,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  DropdownMenuItem(
    text = {
      Text(
          text = "${speed}x",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
      )
    },
    onClick = onClick,
    modifier = modifier,
  )
}

private val VerticalOffsetFactor = 24.dp
private val HorizontalOffsetFactor = 8.dp
private val VerticalPaddingFactor = 12.dp
private val HorizontalPaddingFactor = 6.dp
private val HorizontalOffsetPaddingFactor = 8.dp

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
private fun PlaybackBar(
  session: Session,
  state: AudioPlayer.State,
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
            text = session.title,
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
          session.chapterProgress
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

@Composable
private fun Thumbnail(
  imageUrl: String,
  contentDescription: String?,
  modifier: Modifier = Modifier,
) {
  val painter = rememberAsyncImagePainter(imageUrl)
  val shape = RoundedCornerShape(8.dp)
  Image(
    painter = painter,
    contentDescription = contentDescription,
    modifier = modifier
      .size(ThumbnailSize)
      .clip(shape)
      .border(1.dp, MaterialTheme.colorScheme.secondary, shape),
  )
}

private val ThumbnailSize = 56.dp

private const val SharedBounds = "bounds"
private const val SharedImage = "image"
