package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.campfire.analytics.Analytics
import app.campfire.analytics.events.AudioTrack
import app.campfire.analytics.events.Bookmark
import app.campfire.analytics.events.Changed
import app.campfire.analytics.events.Chapter
import app.campfire.analytics.events.Cleared
import app.campfire.analytics.events.Click
import app.campfire.analytics.events.Forward
import app.campfire.analytics.events.PlayPause
import app.campfire.analytics.events.PlaybackActionEvent
import app.campfire.analytics.events.PlaybackBar
import app.campfire.analytics.events.Rewind
import app.campfire.analytics.events.Seek
import app.campfire.analytics.events.Selected
import app.campfire.analytics.events.SkipNext
import app.campfire.analytics.events.SkipPrevious
import app.campfire.analytics.events.Timer
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.common.compose.theme.colorScheme
import app.campfire.core.extensions.progressOver
import app.campfire.core.logging.bark
import app.campfire.sessions.ui.PlaybackBarState.Collapsed
import app.campfire.sessions.ui.PlaybackBarState.Expanded
import app.campfire.sessions.ui.PlaybackBarState.Hidden
import app.campfire.sessions.ui.expanded.ExpandedPlaybackBar
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.ThemeManager
import com.slack.circuit.runtime.Navigator
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

enum class PlaybackBarState {
  Hidden,
  Collapsed,
  Expanded,
}

internal const val FlingThreshold = 4000f
internal const val TranslationThreshold = 0.75f

internal val BaseShadowElevation = 2.dp
internal val ShadowElevation = 4.dp
internal val TonalElevation = 2.dp

private val DefaultSheetColor: Color
  @Composable get() = MaterialTheme.colorScheme.surfaceContainerHighest
private val DefaultSheetContentColor: Color
  @Composable get() = MaterialTheme.colorScheme.onSurface

private val DefaultNonThemedSheetColor: Color
  @Composable get() = MaterialTheme.colorScheme.secondaryContainer
private val DefaultNonThemedContentColor: Color
  @Composable get() = MaterialTheme.colorScheme.onSecondaryContainer

@OptIn(
  ExperimentalSharedTransitionApi::class,
  ExperimentalCoroutinesApi::class,
  ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
fun PlaybackBar(
  enabled: Boolean,
  expanded: Boolean,
  onExpansionChange: (Boolean) -> Unit,
  navigator: Navigator,
  themeManager: ThemeManager,
  themeSettings: ThemeSettings,
  offset: Density.() -> IntOffset,
  modifier: Modifier = Modifier,
) {
  SessionHostLayout { currentSession, audioPlayer, clearSession, startSession ->

    val currentTime = remember(audioPlayer) {
      audioPlayer?.currentTime
        ?.map {
          // This can be updated with sub-second precision, but we only care to present it
          // to second based granularity so trim the object to prevent unnecessary recompositions
          it.inWholeSeconds.seconds
        }
        ?: emptyFlow()
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

    val isDynamicThemingEnabled by remember {
      themeSettings.observeDynamicallyThemePlayback()
    }.collectAsState()

    val theme = remember(currentSession?.libraryItem?.id, isDynamicThemingEnabled) {
      if (!isDynamicThemingEnabled) {
        flowOf(null)
      } else {
        val itemId = currentSession?.libraryItem?.id
        if (itemId != null) {
          bark("PlaybackBar") { "Observing theme for $itemId" }
          themeManager.observeThemeFor(itemId)
        } else {
          emptyFlow()
        }
      }
    }.collectAsState(null)

    MaterialExpressiveTheme(
      colorScheme = theme.value?.colorScheme,
    ) {
      val sheetContainerColor = if (isDynamicThemingEnabled) {
        DefaultSheetColor
      } else {
        DefaultNonThemedSheetColor
      }

      val sheetContentColor = if (isDynamicThemingEnabled) {
        DefaultSheetContentColor
      } else {
        DefaultNonThemedContentColor
      }

      SharedTransitionLayout(
        modifier = modifier,
      ) {
        AnimatedContent(
          targetState = when {
            !enabled -> Hidden
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
                containerColor = sheetContainerColor,
                contentColor = sheetContentColor,
                session = currentSession,
                state = playerState.value,
                progress = {
                  currentTime.value progressOver currentDuration.value
                },
                currentMetadata = currentMetadata.value,
                runningTimer = runningTimer.value,
                onClick = { onExpansionChange(!expanded) },
                onPlayPauseClick = {
                  Analytics.send(PlaybackActionEvent(PlayPause, Click, PlaybackBar))
                  if (playerState.value == AudioPlayer.State.Disabled) {
                    startSession()
                  } else {
                    audioPlayer?.playPause()
                  }
                },
                onRewindClick = {
                  Analytics.send(PlaybackActionEvent(Rewind, Click, PlaybackBar))
                  audioPlayer?.seekBackward()
                },
                onClearSession = clearSession,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
                modifier = Modifier
                  .padding(8.dp)
                  .offset(offset),
              )
            }

            Expanded -> {
              ExpandedPlaybackBar(
                containerColor = sheetContainerColor,
                contentColor = sheetContentColor,
                navigator = navigator,
                session = currentSession!!,
                state = playerState.value,
                playbackSpeed = playbackSpeed.value,
                currentTime = currentTime.value,
                currentDuration = currentDuration.value,
                currentMetadata = currentMetadata.value,
                runningTimer = runningTimer.value,
                sharedTransitionScope = this@SharedTransitionLayout,
                animatedVisibilityScope = this,
                onPlayPauseClick = {
                  Analytics.send(PlaybackActionEvent(PlayPause, Click, PlaybackBar))
                  if (playerState.value == AudioPlayer.State.Disabled) {
                    startSession()
                  } else {
                    audioPlayer?.playPause()
                  }
                },
                onRewindClick = {
                  Analytics.send(PlaybackActionEvent(Rewind, Click, PlaybackBar))
                  audioPlayer?.seekBackward()
                },
                onForwardClick = {
                  Analytics.send(PlaybackActionEvent(Forward, Click, PlaybackBar))
                  audioPlayer?.seekForward()
                },
                onSkipPreviousClick = {
                  Analytics.send(PlaybackActionEvent(SkipPrevious, Click, PlaybackBar))
                  audioPlayer?.skipToPrevious()
                },
                onSkipNextClick = {
                  Analytics.send(PlaybackActionEvent(SkipNext, Click, PlaybackBar))
                  audioPlayer?.skipToNext()
                },
                onClose = { onExpansionChange(false) },
                onSeek = { progress ->
                  Analytics.send(
                    PlaybackActionEvent(
                      Seek,
                      Changed,
                      PlaybackBar,
                      extras = mapOf("progress" to progress),
                    ),
                  )
                  audioPlayer?.seekTo(progress)
                },
                onTimerCleared = {
                  Analytics.send(PlaybackActionEvent(Timer, Cleared, PlaybackBar))
                  audioPlayer?.clearTimer()
                },
                onTimerSelected = { timer ->
                  Analytics.send(
                    PlaybackActionEvent(
                      obj = Timer,
                      verb = Changed,
                      noun = PlaybackBar,
                      extras = when (timer) {
                        is PlaybackTimer.EndOfChapter -> mapOf("type" to "end_of_chapter")
                        is PlaybackTimer.Epoch -> mapOf(
                          "type" to "epoch",
                          "time" to timer.epochMillis,
                        )
                      },
                    ),
                  )
                  audioPlayer?.setTimer(timer)
                },
                onChapterSelected = { chapter ->
                  Analytics.send(PlaybackActionEvent(Chapter, Selected))
                  audioPlayer?.seekTo(chapter.id)
                },
                onAudioTrackSelected = { track ->
                  Analytics.send(PlaybackActionEvent(AudioTrack, Selected))
                  audioPlayer?.seekTo(track.index - 1)
                },
                onBookmarkSelected = { bookmark ->
                  Analytics.send(PlaybackActionEvent(Bookmark, Selected))
                  audioPlayer?.seekTo(bookmark.time)
                },
              )
            }
          }
        }
      }
    }
  }
}

internal const val SharedBounds = "bounds"
internal const val SharedImage = "image"
