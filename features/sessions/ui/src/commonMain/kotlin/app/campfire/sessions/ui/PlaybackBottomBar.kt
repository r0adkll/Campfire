package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.rounded.EditAudio
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import app.campfire.sessions.ui.composables.RunningTimerText
import app.campfire.sessions.ui.sheets.chapters.ChapterResult
import app.campfire.sessions.ui.sheets.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.sheets.speed.showPlaybackSpeedBottomSheet
import app.campfire.sessions.ui.sheets.timer.TimerResult
import app.campfire.sessions.ui.sheets.timer.showTimerBottomSheet
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.label_end_of_chapter_short
import com.slack.circuit.overlay.LocalOverlayHost
import ir.mahozad.multiplatform.wavyslider.WaveDirection
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PlaybackBottomBar(
  modifier: Modifier = Modifier,
) {
  SessionHostLayout { currentSession, audioPlayer, clearSession ->
    val currentTime = remember(audioPlayer) {
      audioPlayer?.currentTime ?: emptyFlow()
    }.collectAsState(0.seconds)

    val currentDuration = remember(audioPlayer) {
      audioPlayer?.currentDuration ?: emptyFlow()
    }.collectAsState(0.seconds)

    val currentMetadata = remember(audioPlayer) {
      audioPlayer?.currentMetadata ?: emptyFlow()
    }.collectAsState(Metadata())

    val playerState = remember(audioPlayer) {
      audioPlayer?.state ?: emptyFlow()
    }.collectAsState(AudioPlayer.State.Disabled)

    val playbackSpeed = remember(audioPlayer) {
      audioPlayer?.playbackSpeed ?: emptyFlow()
    }.collectAsState(1f)

    val runningTimer = remember(audioPlayer) {
      audioPlayer?.runningTimer ?: emptyFlow()
    }.collectAsState(null)

    PlaybackBottomBar(
      session = currentSession,
      state = playerState.value,
      playbackSpeed = playbackSpeed.value,
      currentTime = currentTime.value,
      currentDuration = currentDuration.value,
      currentMetadata = currentMetadata.value,
      runningTimer = runningTimer.value,
      onPlayPauseClick = { audioPlayer?.playPause() },
      onRewindClick = { audioPlayer?.seekBackward() },
      onForwardClick = { audioPlayer?.seekForward() },
      onSkipPreviousClick = { audioPlayer?.skipToPrevious() },
      onSkipNextClick = { audioPlayer?.skipToNext() },
      onSeek = { progress ->
        audioPlayer?.seekTo(progress)
      },
      onTimerCleared = {
        audioPlayer?.clearTimer()
      },
      onTimerSelected = { timer ->
        audioPlayer?.setTimer(timer)
      },
      onChapterSelected = { chapter ->
        audioPlayer?.seekTo(chapter.id)
      },
      modifier = modifier,
    )
  }
}

@Composable
private fun PlaybackBottomBar(
  state: AudioPlayer.State,
  playbackSpeed: Float,
  currentTime: Duration,
  currentDuration: Duration,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,

  session: Session?,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeek: (Float) -> Unit,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  onChapterSelected: (Chapter) -> Unit,

  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left Grouping of items, mainly the image, title, subtitle, and timer
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        ItemThumbnailImage(
          session = session,
          currentMetadata = currentMetadata,
          modifier = Modifier.padding(16.dp),
        )

        Column(
          verticalArrangement = Arrangement.Center,
        ) {
          Text(
            text = currentMetadata.title ?: "--",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            fontFamily = PaytoneOneFontFamily,
            modifier = Modifier,
          )

          Text(
            text = session?.libraryItem?.media?.metadata?.title ?: "--",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(40f),
          )
        }
      }

      val interactionSource = remember { MutableInteractionSource() }
      val isPressed by interactionSource.collectIsPressedAsState()
      val isDragged by interactionSource.collectIsDraggedAsState()
      val isInteracting = isPressed || isDragged

      Column(
        modifier = Modifier
          .weight(3f)
          .padding(
            vertical = 8.dp,
          ),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        PlaybackActions(
          state = state,
          isInteracting = isInteracting,
          onSkipPreviousClick = onSkipPreviousClick,
          onRewindClick = onRewindClick,
          onPlayPauseClick = onPlayPauseClick,
          onForwardClick = onForwardClick,
          onSkipNextClick = onSkipNextClick,
        )

        PlaybackSeekBar(
          state = state,
          currentTime = currentTime,
          currentDuration = currentDuration,
          onSeek = onSeek,
          modifier = Modifier
            .padding(horizontal = 24.dp),
          interactionSource = interactionSource,
        )
      }

      val overlayHost = LocalOverlayHost.current
      ActionRow(
        modifier = Modifier.weight(1f),
        runningTimer = runningTimer,
        onBookmarkAddClick = {},
        onSpeedClick = {
          scope.launch {
            overlayHost.showPlaybackSpeedBottomSheet(playbackSpeed)
          }
        },
        onTimerClick = {
          if (session == null) return@ActionRow
          scope.launch {
            when (val result = overlayHost.showTimerBottomSheet(runningTimer)) {
              is TimerResult.Selected -> onTimerSelected(result.timer)
              TimerResult.Cleared -> onTimerCleared()
              else -> Unit
            }
          }
        },
        onChapterListClick = {
          if (session == null) return@ActionRow
          scope.launch {
            val result = overlayHost.showChapterBottomSheet(session.libraryItem.media.chapters)
            if (result is ChapterResult.Selected) {
              onChapterSelected(result.chapter)
            }
          }
        },
      )
    }
  }
}

@Composable
private fun ItemThumbnailImage(
  session: Session?,
  currentMetadata: Metadata,
  modifier: Modifier = Modifier,
) {
  val imageSize = 88.dp
  val mediaUrl = currentMetadata.artworkUri
    ?: session?.libraryItem?.media?.coverImageUrl
    ?: ""
  CoverImage(
    imageUrl = mediaUrl,
    contentDescription = session?.libraryItem?.media?.metadata?.title,
    size = imageSize,
    shape = RoundedCornerShape(8.dp),
    modifier = modifier,
  )
}

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
  actionSize: Dp = 32.dp,
  playPauseSize: Dp = 48.dp,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
  ) {
    IconButton(
      onClick = onSkipPreviousClick,
    ) {
      Icon(
        Icons.Rounded.SkipPrevious,
        modifier = Modifier.size(actionSize),
        contentDescription = null,
      )
    }

    IconButton(
      onClick = onRewindClick,
    ) {
      Icon(
        Icons.Rounded.Replay10,
        modifier = Modifier.size(actionSize),
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
      },
    )

    Surface(
      shape = CircleShape,
      modifier = Modifier.size(playPauseSize),
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
              .size(actionSize)
              .alpha(if (isPlayPauseEnabled) 1f else 0.5f),
            contentDescription = null,
          )
        } else {
          CircularProgressIndicator(
            modifier = Modifier.size(actionSize),
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
        modifier = Modifier.size(actionSize),
        contentDescription = null,
      )
    }

    IconButton(
      onClick = onSkipNextClick,
    ) {
      Icon(
        Icons.Rounded.SkipNext,
        modifier = Modifier.size(actionSize),
        contentDescription = null,
      )
    }
  }
}

@Composable
private fun PlaybackSeekBar(
  state: AudioPlayer.State,
  currentTime: Duration,
  currentDuration: Duration,
  onSeek: (Float) -> Unit,
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
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

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    val currentTimeLabel = if (isInteracting) {
      currentDuration.times(sliderValue.toDouble()).readoutFormat()
    } else {
      currentTime.readoutFormat()
    }

    Text(
      text = currentTimeLabel,
      textAlign = TextAlign.End,
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .width(100.dp),
    )

    val sliderColors = SliderDefaults.colors(
      inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainer,
    )
    WavySlider(
      value = softSliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = {
        onSeek(sliderValue)
      },
      interactionSource = interactionSource,
      colors = sliderColors,
      waveLength = 50.dp,
      waveHeight = waveHeight,
      waveVelocity = waveVelocity to WaveDirection.TAIL,
      waveThickness = waveThickness,
      thumb = {
        SliderDefaults.Thumb(
          interactionSource = interactionSource,
          colors = sliderColors,
          enabled = true,
          thumbSize = DpSize(4.dp, 36.dp),
        )
      },
      modifier = Modifier
        .width(640.dp)
        .padding(horizontal = 24.dp),
    )

    val currentRemainingDuration = currentDuration - currentTime
    Text(
      text = currentRemainingDuration.readoutFormat(),
      textAlign = TextAlign.Start,
      style = MaterialTheme.typography.labelSmall,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier
        .width(100.dp),
    )
  }
}

@Composable
private fun ActionRow(
  runningTimer: RunningTimer?,
  onBookmarkAddClick: () -> Unit,
  onSpeedClick: () -> Unit,
  onTimerClick: () -> Unit,
  onChapterListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(
      horizontal = 16.dp,
    ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
  ) {
    IconButton(
      onClick = onBookmarkAddClick,
    ) {
      Icon(Icons.Outlined.BookmarkAdd, contentDescription = null)
    }

    IconButton(
      onClick = onSpeedClick,
    ) {
      Icon(Icons.Rounded.Speed, contentDescription = null)
    }

    AnimatedContent(
      targetState = runningTimer,
      transitionSpec = {
        (
          fadeIn(animationSpec = tween(220, delayMillis = 90)) +
            scaleIn(
              initialScale = 0.92f,
              animationSpec = tween(220, delayMillis = 90),
              transformOrigin = TransformOrigin(0.1f, 0.5f),
            )
          )
          .togetherWith(fadeOut(animationSpec = tween(90)))
      },
      contentAlignment = Alignment.CenterStart,
    ) { timer ->
      if (timer == null) {
        IconButton(
          onClick = onTimerClick,
        ) {
          Icon(Icons.Outlined.Timer, contentDescription = null)
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable {
              onTimerClick()
            }
            .background(
              color = MaterialTheme.colorScheme.primary,
              shape = RoundedCornerShape(50),
            ),
        ) {
          CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onPrimary,
          ) {
            Box(
              modifier = Modifier.size(40.dp),
              contentAlignment = Alignment.Center,
            ) {
              Icon(Icons.Outlined.Timer, contentDescription = null)
            }
            RunningTimerText(
              runningTimer = timer,
              endOfChapterText = stringResource(Res.string.label_end_of_chapter_short),
              style = {
                when (it) {
                  PlaybackTimer.EndOfChapter -> MaterialTheme.typography.labelMedium
                  is PlaybackTimer.Epoch -> MaterialTheme.typography.labelMedium
                }
              },
            )
            Spacer(Modifier.width(16.dp))
          }
        }
      }
    }

    IconButton(
      onClick = onChapterListClick,
    ) {
      Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
    }
  }
}
