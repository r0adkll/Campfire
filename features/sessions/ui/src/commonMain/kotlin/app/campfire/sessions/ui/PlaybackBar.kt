package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.sessions.ui.PlaybackBarState.Collapsed
import app.campfire.sessions.ui.PlaybackBarState.Expanded
import app.campfire.sessions.ui.PlaybackBarState.Hidden
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.emptyFlow

enum class PlaybackBarState {
  Hidden,
  Collapsed,
  Expanded,
}

internal const val FlingThreshold = 4000f
internal const val TranslationThreshold = 0.75f

internal val ShadowElevation = 4.dp
internal val TonalElevation = 2.dp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PlaybackBar(
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
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
            CollapsedPlaybackBar(
              session = currentSession,
              state = playerState.value,
              currentTime = currentTime.value,
              currentDuration = currentDuration.value,
              currentMetadata = currentMetadata.value,
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
              state = playerState.value,
              playbackSpeed = playbackSpeed.value,
              currentTime = currentTime.value,
              currentDuration = currentDuration.value,
              currentMetadata = currentMetadata.value,
              runningTimer = runningTimer.value,
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
              onTimerCleared = {
                audioPlayer?.clearTimer()
              },
              onTimerSelected = { timer ->
                audioPlayer?.setTimer(timer)
              },
              onChapterSelected = { chapter ->
                audioPlayer?.seekTo(chapter.id)
              },
            )
          }
        }
      }
    }
  }
}

internal const val SharedBounds = "bounds"
internal const val SharedImage = "image"
