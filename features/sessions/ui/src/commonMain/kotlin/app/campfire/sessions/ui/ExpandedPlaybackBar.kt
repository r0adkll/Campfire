package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowRight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.TimerResult
import app.campfire.audioplayer.ui.cast.CastButton
import app.campfire.audioplayer.ui.showTimerBottomSheet
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmarks
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.icons.rounded.ShakeVeryHigh
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.CoverImageSize
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.sessions.ui.composables.ForwardIcon
import app.campfire.sessions.ui.composables.PlaybackSpeedAction
import app.campfire.sessions.ui.composables.RewindIcon
import app.campfire.sessions.ui.composables.RunningTimerAction
import app.campfire.sessions.ui.composables.RunningTimerText
import app.campfire.sessions.ui.sheets.bookmarks.BookmarkResult
import app.campfire.sessions.ui.sheets.bookmarks.showBookmarksBottomSheet
import app.campfire.sessions.ui.sheets.chapters.ChapterResult
import app.campfire.sessions.ui.sheets.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.sheets.speed.showPlaybackSpeedBottomSheet
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val ExpandedVerticalOffsetFactor = 56.dp
private val ExpandedHorizontalOffsetFactor = 4.dp
private val ExpandedCornerRadiusFactor = 24.dp
internal val LargeCoverImageSize = 188.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ExpandedPlaybackBar(
  containerColor: Color,
  contentColor: Color,
  navigator: Navigator,

  state: AudioPlayer.State,
  playbackSpeed: Float,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,

  session: Session,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeek: (Float) -> Unit,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  onChapterSelected: (Chapter) -> Unit,
  onBookmarkSelected: (Bookmark) -> Unit,

  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  val overlayHost = rememberOverlayHost()
  ContentWithOverlays(
    overlayHost = overlayHost,
    modifier = modifier,
  ) {
    ExpandedPlaybackBar(
      containerColor = containerColor,
      contentColor = contentColor,
      navigator = navigator,
      overlayHost = overlayHost,
      state = state,
      playbackSpeed = playbackSpeed,
      currentTime = currentTime,
      currentDuration = currentDuration,
      currentMetadata = currentMetadata,
      runningTimer = runningTimer,
      session = session,
      onPlayPauseClick = onPlayPauseClick,
      onRewindClick = onRewindClick,
      onForwardClick = onForwardClick,
      onSkipNextClick = onSkipNextClick,
      onSkipPreviousClick = onSkipPreviousClick,
      onSeek = onSeek,
      onTimerCleared = onTimerCleared,
      onTimerSelected = onTimerSelected,
      onChapterSelected = onChapterSelected,
      onBookmarkSelected = onBookmarkSelected,
      onClose = onClose,
      sharedTransitionScope = sharedTransitionScope,
      animatedVisibilityScope = animatedVisibilityScope,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun ExpandedPlaybackBar(
  containerColor: Color,
  contentColor: Color,
  navigator: Navigator,
  overlayHost: OverlayHost,
  state: AudioPlayer.State,
  playbackSpeed: Float,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,

  session: Session,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeek: (Float) -> Unit,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  onChapterSelected: (Chapter) -> Unit,
  onBookmarkSelected: (Bookmark) -> Unit,

  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) = with(sharedTransitionScope) {
  val windowSizeClass = LocalWindowSizeClass.current
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

  val isDisposing by remember {
    derivedStateOf { easedOffset > TranslationThreshold }
  }

  val hapticFeedback = LocalHapticFeedback.current
  LaunchedEffect(isDisposing) {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
  }

  Surface(
    color = containerColor,
    contentColor = contentColor,
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
        systemBarsPadding()
          .padding(top = 32.dp)
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
        actions = {
          CastButton()
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = containerColor,
          navigationIconContentColor = MaterialTheme.colorScheme.contentColorFor(containerColor),
          actionIconContentColor = MaterialTheme.colorScheme.contentColorFor(containerColor),
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
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          ExpandedItemImage(
            currentMetadata = currentMetadata,
            runningTimer = runningTimer,
            session = session,
            animatedVisibilityScope = animatedVisibilityScope,
            size = if (windowSizeClass.isSupportingPaneEnabled) {
              LargeCoverImageSize
            } else {
              CoverImageSize
            },
            modifier = Modifier.clickable {
              scope.launch {
                onClose()
                delay(350L)
                navigator.goTo(LibraryItemScreen(session.libraryItem.id))
              }
            },
          )

          Spacer(Modifier.height(16.dp))

          Text(
            text = currentMetadata.title ?: session.title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PaytoneOneFontFamily,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .padding(horizontal = 24.dp),
          )

          Text(
            text = session.libraryItem.media.metadata.title ?: "",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
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

        PlaybackSeekBar(
          state = state,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          onSeek = onSeek,
          interactionSource = interactionSource,
        )

        Spacer(Modifier.height(24.dp))

        PlaybackActions(
          state = state,
          isInteracting = isInteracting,
          onSkipPreviousClick = onSkipPreviousClick,
          onRewindClick = onRewindClick,
          onPlayPauseClick = onPlayPauseClick,
          onForwardClick = onForwardClick,
          onSkipNextClick = onSkipNextClick,
        )
      }

      Spacer(Modifier.height(16.dp))

      ActionRow(
        onBookmarksClick = {
          scope.launch {
            when (val result = overlayHost.showBookmarksBottomSheet(session.libraryItem.id)) {
              is BookmarkResult.Selected -> onBookmarkSelected(result.bookmark)
              BookmarkResult.None -> Unit
            }
          }
        },
        speedContent = {
          PlaybackSpeedAction(
            playbackSpeed = playbackSpeed,
            onClick = {
              scope.launch {
                overlayHost.showPlaybackSpeedBottomSheet(playbackSpeed)
              }
            },
          )
        },
        timerContent = {
          RunningTimerAction(
            runningTimer = runningTimer,
            currentTime = currentTime,
            currentDuration = currentDuration,
            playbackSpeed = playbackSpeed,
            onClick = {
              scope.launch {
                when (val result = overlayHost.showTimerBottomSheet(runningTimer)) {
                  is TimerResult.Selected -> onTimerSelected(result.timer)
                  TimerResult.Cleared -> onTimerCleared()
                  else -> Unit
                }
              }
            },
          )
        },
        onChapterListClick = {
          scope.launch {
            val result = overlayHost.showChapterBottomSheet(
              chapters = session.libraryItem.media.chapters,
              currentChapter = session.chapter,
              playbackSpeed = playbackSpeed,
            )
            if (result is ChapterResult.Selected) {
              onChapterSelected(result.chapter)
            }
          }
        },
      )

      Spacer(Modifier.navigationBarsPadding())
    }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedTransitionScope.ExpandedItemImage(
  session: Session,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,
  animatedVisibilityScope: AnimatedVisibilityScope,
  size: Dp,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier,
  ) {
    val mediaUrl = currentMetadata.artworkUri
      ?: session.libraryItem.media.coverImageUrl
    CoverImage(
      imageUrl = mediaUrl,
      contentDescription = session.libraryItem.media.metadata.title,
      size = size,
      modifier = Modifier.sharedElement(
        rememberSharedContentState(SharedImage),
        animatedVisibilityScope = animatedVisibilityScope,
      ),
    )

    AnimatedVisibility(
      visible = runningTimer != null,
      enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
      modifier = Modifier.size(size),
    ) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(size)
          .background(Color.Black.copy(0.3f), RoundedCornerShape(32.dp)),
      ) {
        if (runningTimer?.isShakeToRestartEnabled == true) {
          Icon(
            CampfireIcons.Rounded.ShakeVeryHigh,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
              .align(Alignment.TopEnd)
              .padding(16.dp),
          )
        }

        if (runningTimer != null) {
          RunningTimerText(
            runningTimer = runningTimer,
            color = Color.White,
            modifier = Modifier.fillMaxWidth().padding(16.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun PlaybackSeekBar(
  state: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
  Column(
    modifier = modifier,
  ) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val isDragged by interactionSource.collectIsDraggedAsState()
    val isInteracting = isPressed || isDragged

    fun calculateProgress(): Float = if (currentDuration.inWholeMilliseconds == 0L) {
      0f
    } else {
      (currentTime / currentDuration).toFloat()
    }

    var sliderValue by remember { mutableStateOf(calculateProgress()) }
    val softSliderValue by animateFloatAsState(sliderValue)
    LaunchedEffect(isInteracting, state, currentTime, currentDuration) {
      if (!isInteracting) {
        sliderValue = calculateProgress()
      }
    }

    val waveHeight = if (state == AudioPlayer.State.Playing) 16.dp else 0.dp
    val waveVelocity = if (state == AudioPlayer.State.Playing) 40.dp else 0.dp
    val waveThickness = if (state == AudioPlayer.State.Playing) 12.dp else 16.dp

    WavySlider(
      enabled = state == AudioPlayer.State.Playing || state == AudioPlayer.State.Paused,
      value = softSliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = {
        onSeek(sliderValue)
      },
      interactionSource = interactionSource,
      colors = SliderDefaults.colors(
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
      ),
      waveLength = 50.dp,
      waveHeight = waveHeight,
      waveVelocity = waveVelocity to WaveDirection.TAIL,
      waveThickness = waveThickness,
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

      val isAccelerated = playbackSpeed != 1f
      AnimatedVisibility(
        visible = isAccelerated,
      ) {
        Icon(
          Icons.Rounded.KeyboardDoubleArrowRight,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.secondary,
        )
      }

      val currentRemainingDuration = (currentDuration - currentTime).div(playbackSpeed.toDouble())
      Text(
        text = currentRemainingDuration.readoutFormat(),
        style = MaterialTheme.typography.labelSmall.fluentIf(isAccelerated) {
          copy(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.secondary,
            fontSize = 12.sp,
          )
        },
      )
    }
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaybackActions(
  state: AudioPlayer.State,
  isInteracting: Boolean,
  onSkipPreviousClick: () -> Unit,
  onRewindClick: () -> Unit,
  onPlayPauseClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    val playButtonExtraWidth = 32.dp
    val playButtonSize = ButtonDefaults.LargeContainerHeight
    val accessoryButtonContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
    val accessoryButtonContentColor = MaterialTheme.colorScheme.onSurface

    Row(
      modifier = modifier,
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
      val buttonShape = MaterialTheme.shapes.extraLarge

      @Composable
      fun AccessoryButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        content: @Composable (iconSize: Dp) -> Unit,
      ) {
        val accessoryButtonSize = ButtonDefaults.LargeContainerHeight
        val accessoryButtonIconSize = ButtonDefaults.LargeIconSize
        FilledIconButton(
          onClick = onClick,
          shapes = IconButtonDefaults.shapes(
            shape = buttonShape,
            pressedShape = ButtonDefaults.shape,
          ),
          colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = accessoryButtonContainerColor,
            contentColor = accessoryButtonContentColor,
          ),
          modifier = modifier
            .sizeIn(
              minWidth = accessoryButtonSize,
              minHeight = accessoryButtonSize,
            ),
          content = {
            content(accessoryButtonIconSize)
          },
        )
      }

      AccessoryButton(
        onClick = onRewindClick,
      ) { iconSize ->
        RewindIcon(
          modifier = Modifier.size(iconSize),
        )
      }

      val isPlayPauseEnabled = state != AudioPlayer.State.Finished &&
        state != AudioPlayer.State.Buffering &&
        !isInteracting

      val playButtonIconSize = ButtonDefaults.ExtraLargeIconSize
      FilledIconButton(
        onClick = onPlayPauseClick,
        enabled = isPlayPauseEnabled,
        shapes = IconButtonDefaults.shapes(
          shape = buttonShape,
          pressedShape = ButtonDefaults.shape,
        ),
        modifier = Modifier
          .sizeIn(
            minWidth = playButtonSize + playButtonExtraWidth,
            minHeight = playButtonSize,
          ),
      ) {
        AnimatedContent(
          targetState = when {
            state == AudioPlayer.State.Buffering -> PlayButtonState.Buffering
            isInteracting -> PlayButtonState.Interacting
            state == AudioPlayer.State.Playing -> PlayButtonState.Playing
            else -> PlayButtonState.Paused
          },
          transitionSpec = {
            (fadeIn(initialAlpha = 0.4f) + expandIn(expandFrom = Alignment.Center)) togetherWith
              (fadeOut(targetAlpha = 0.4f) + shrinkOut(shrinkTowards = Alignment.Center))
          },
          contentAlignment = Alignment.Center,
        ) { state ->
          if (state == PlayButtonState.Buffering) {
            CircularProgressIndicator(
              modifier = Modifier.size(playButtonIconSize),
              strokeWidth = 4.dp,
            )
          } else {
            Icon(
              when (state) {
                PlayButtonState.Interacting -> Icons.Rounded.EditAudio
                PlayButtonState.Playing -> Icons.Rounded.Pause
                else -> Icons.Rounded.PlayArrow
              },
              modifier = Modifier.size(playButtonIconSize),
              contentDescription = null,
            )
          }
        }
      }

      AccessoryButton(
        onClick = onForwardClick,
      ) { iconSize ->
        ForwardIcon(
          modifier = Modifier.size(iconSize),
        )
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      modifier = Modifier
        .widthIn(
          max = (ButtonDefaults.LargeContainerHeight * 3) + playButtonExtraWidth + 8.dp,
        ),
    ) {
      val buttonSize = ButtonDefaults.MinHeight
      val colors = IconButtonDefaults.filledIconButtonColors(
        containerColor = accessoryButtonContainerColor,
        contentColor = accessoryButtonContentColor,
      )
      val shapes = IconButtonDefaults.shapes()

      FilledIconButton(
        onClick = onSkipPreviousClick,
        shapes = shapes,
        colors = colors,
        modifier = Modifier
          .weight(1f)
          .heightIn(buttonSize),
      ) {
        Icon(
          Icons.Rounded.SkipPrevious,
          modifier = Modifier.size(ButtonDefaults.LargeIconSize),
          contentDescription = null,
        )
      }
      FilledIconButton(
        onClick = onSkipNextClick,
        shapes = shapes,
        colors = colors,
        modifier = Modifier
          .weight(1f)
          .heightIn(buttonSize),
      ) {
        Icon(
          Icons.Rounded.SkipNext,
          modifier = Modifier.size(ButtonDefaults.LargeIconSize),
          contentDescription = null,
        )
      }
    }
  }
}

@Composable
private fun ActionRow(
  onBookmarksClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  timerContent: @Composable () -> Unit,
  onChapterListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .height(72.dp)
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceEvenly,
  ) {
    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        onClick = onBookmarksClick,
      ) {
        Icon(Icons.Rounded.Bookmarks, contentDescription = null)
      }
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      speedContent()
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      timerContent()
    }

    Box(
      modifier = Modifier.weight(1f),
      contentAlignment = Alignment.Center,
    ) {
      IconButton(
        onClick = onChapterListClick,
      ) {
        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
      }
    }
  }
}

enum class PlayButtonState {
  Buffering,
  Interacting,
  Playing,
  Paused,
}
