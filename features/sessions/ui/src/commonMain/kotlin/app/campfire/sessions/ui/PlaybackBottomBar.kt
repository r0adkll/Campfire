// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmarks
import app.campfire.common.compose.icons.rounded.Equalizer
import app.campfire.common.compose.icons.rounded.List
import app.campfire.common.compose.icons.rounded.Timer
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.di.ComponentHolder
import app.campfire.core.di.UserScope
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import app.campfire.sessions.ui.bottombar.BookTimeline
import app.campfire.sessions.ui.bottombar.DesktopPlaybackActions
import app.campfire.sessions.ui.composables.PlaybackSpeedAction
import app.campfire.sessions.ui.composables.RunningTimerText
import app.campfire.sessions.ui.playback.PlaybackPresenterFactory
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.PlayerUiEvent
import app.campfire.sessions.ui.sheets.bookmarks.BookmarkResult
import app.campfire.sessions.ui.sheets.bookmarks.showBookmarksBottomSheet
import app.campfire.sessions.ui.sheets.chapters.ChapterResult
import app.campfire.sessions.ui.sheets.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.sheets.equalizer.showEqualizerBottomSheet
import app.campfire.sessions.ui.sheets.sleeptimer.TimerResult
import app.campfire.sessions.ui.sheets.sleeptimer.showSleepTimerBottomSheet
import app.campfire.sessions.ui.sheets.speed.showPlaybackSpeedBottomSheet
import app.campfire.sessions.ui.sheets.tracks.AudioTrackResult
import app.campfire.sessions.ui.sheets.tracks.showAudioTrackBottomSheet
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_add_bookmark
import campfire.features.sessions.ui.generated.resources.action_chapters
import campfire.features.sessions.ui.generated.resources.action_equalizer
import campfire.features.sessions.ui.generated.resources.action_sleep_timer
import campfire.features.sessions.ui.generated.resources.bottom_bar_chapter_remaining
import campfire.features.sessions.ui.generated.resources.bottom_bar_nothing_playing
import campfire.features.sessions.ui.generated.resources.bottom_bar_nothing_playing_hint
import campfire.features.sessions.ui.generated.resources.label_end_of_chapter_short
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.LocalOverlayHost
import com.slack.circuit.retained.rememberRetained
import kotlin.time.Duration
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@ContributesTo(UserScope::class)
interface PlaybackBottomBarComponent {
  val playbackPresenterFactory: PlaybackPresenterFactory
}

@Composable
private fun rememberPlaybackBottomBarComponent(): State<PlaybackBottomBarComponent> {
  return remember {
    ComponentHolder.subscribe<PlaybackBottomBarComponent>()
  }.collectAsState(ComponentHolder.component<PlaybackBottomBarComponent>())
}

/**
 * The always-visible desktop player, spanning the window's bottom edge. Two rows: a thin
 * whole-book timeline with chapter and bookmark markers on top, and cover, titles, transport,
 * and tools beneath. Renders a "nothing playing" state instead of disappearing, so the window
 * layout stays put between sessions.
 */
@Composable
fun PlaybackBottomBar(
  modifier: Modifier = Modifier,
  component: State<PlaybackBottomBarComponent> = rememberPlaybackBottomBarComponent(),
) {
  val comp by component
  key(comp) {
    val presenter = rememberRetained {
      comp.playbackPresenterFactory()
    }

    // The desktop bar has no expanded form, so it never re-primes the sync observation
    PlaybackBottomBar(
      uiState = presenter.present(expanded = false),
      modifier = modifier,
    )
  }
}

@Composable
private fun PlaybackBottomBar(
  uiState: PlaybackUiState,
  modifier: Modifier = Modifier,
) {
  val playerState = uiState.playerState
  PlaybackBottomBarContent(
    session = uiState.session,
    state = playerState.state,
    playbackSpeed = playerState.speed,
    currentTime = playerState.time,
    currentDuration = playerState.duration,
    bookTime = playerState.bookTime,
    currentMetadata = playerState.metadata,
    runningTimer = playerState.timer,
    equalizer = playerState.equalizer,
    bookmarks = playerState.bookmarks,
    onPlayPauseClick = { playerState.eventSink(PlayerUiEvent.PlayPauseClick) },
    onRewindClick = { playerState.eventSink(PlayerUiEvent.RewindClick) },
    onForwardClick = { playerState.eventSink(PlayerUiEvent.FastForwardClick) },
    onSkipPreviousClick = { playerState.eventSink(PlayerUiEvent.PreviousClick) },
    onSkipNextClick = { playerState.eventSink(PlayerUiEvent.NextClick) },
    onSeekTo = { time -> playerState.eventSink(PlayerUiEvent.Seek.Position(time)) },
    onTimerCleared = { playerState.eventSink(PlayerUiEvent.ClearTimer) },
    onTimerSelected = { timer -> playerState.eventSink(PlayerUiEvent.TimerSelected(timer)) },
    onChapterSelected = { chapter -> playerState.eventSink(PlayerUiEvent.ChapterSelected(chapter)) },
    onAudioTrackSelected = { track -> playerState.eventSink(PlayerUiEvent.AudioTrackSelected(track)) },
    onBookmarkSelected = { bookmark -> playerState.eventSink(PlayerUiEvent.BookmarkSelected(bookmark)) },
    modifier = modifier,
  )
}

/** The bar itself, driven by plain values so it can be previewed and rendered in tests. */
@Composable
internal fun PlaybackBottomBarContent(
  state: AudioPlayer.State,
  playbackSpeed: Float,
  currentTime: Duration,
  currentDuration: Duration,
  bookTime: Duration,
  currentMetadata: Metadata,
  runningTimer: RunningTimer?,
  equalizer: EqualizerState,
  bookmarks: List<Bookmark>,

  session: Session?,
  onPlayPauseClick: () -> Unit,
  onRewindClick: () -> Unit,
  onForwardClick: () -> Unit,
  onSkipNextClick: () -> Unit,
  onSkipPreviousClick: () -> Unit,
  onSeekTo: (Duration) -> Unit,
  onTimerSelected: (PlaybackTimer) -> Unit,
  onTimerCleared: () -> Unit,
  onChapterSelected: (Chapter) -> Unit,
  onAudioTrackSelected: (AudioTrack) -> Unit,
  onBookmarkSelected: (Bookmark) -> Unit,

  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val hasSession = session != null
  val chapters = session?.libraryItem?.media?.chapters.orEmpty()
  val hasChapters = chapters.isNotEmpty() && session?.episodeId == null

  Surface(
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = modifier,
  ) {
    Column {
      BookTimeline(
        position = bookTime,
        duration = session?.duration ?: Duration.ZERO,
        chapters = if (hasChapters) chapters else emptyList(),
        bookmarks = bookmarks,
        playbackSpeed = playbackSpeed,
        enabled = hasSession && state != AudioPlayer.State.Initializing,
        onSeek = onSeekTo,
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = TimelineTopPadding)
          .padding(horizontal = 8.dp),
      )

      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .fillMaxWidth()
          .height(ContentRowHeight)
          .padding(start = 16.dp, end = 8.dp, top = TimelineBottomPadding, bottom = 8.dp),
      ) {
        NowPlayingInfo(
          session = session,
          currentMetadata = currentMetadata,
          currentTime = currentTime,
          currentDuration = currentDuration,
          playbackSpeed = playbackSpeed,
          hasChapters = hasChapters,
          modifier = Modifier.weight(1f),
        )

        DesktopPlaybackActions(
          state = state,
          enabled = hasSession,
          onSkipPreviousClick = onSkipPreviousClick,
          onRewindClick = onRewindClick,
          onPlayPauseClick = onPlayPauseClick,
          onForwardClick = onForwardClick,
          onSkipNextClick = onSkipNextClick,
        )

        val overlayHost = LocalOverlayHost.current
        ActionRow(
          modifier = Modifier.weight(1f),
          enabled = hasSession,
          runningTimer = runningTimer,
          onBookmarkAddClick = {
            if (session == null) return@ActionRow
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
                if (session == null) return@PlaybackSpeedAction
                scope.launch {
                  overlayHost.showPlaybackSpeedBottomSheet(session.libraryItem.id, playbackSpeed)
                }
              },
            )
          },
          onTimerClick = {
            if (session == null) return@ActionRow
            scope.launch {
              when (val result = overlayHost.showSleepTimerBottomSheet(runningTimer)) {
                is TimerResult.Selected -> onTimerSelected(result.timer)
                TimerResult.Cleared -> onTimerCleared()
                else -> Unit
              }
            }
          },
          showEqualizer = equalizer !is EqualizerState.Unsupported,
          onEqualizerClick = {
            if (session == null) return@ActionRow
            scope.launch {
              overlayHost.showEqualizerBottomSheet(session.libraryItem.id)
            }
          },
          showChapters = session?.episodeId == null,
          onChapterListClick = {
            if (session == null) return@ActionRow
            if (session.libraryItem.media.chapters.isNotEmpty()) {
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
            } else if (session.libraryItem.media.tracks.isNotEmpty()) {
              scope.launch {
                val result = overlayHost.showAudioTrackBottomSheet(
                  audioTracks = session.libraryItem.media.tracks,
                  currentAudioTrack = session.audioTrack,
                  playbackSpeed = playbackSpeed,
                )
                if (result is AudioTrackResult.Selected) {
                  onAudioTrackSelected(result.audioTrack)
                }
              }
            }
          },
        )
      }
    }
  }
}

/** Cover, current chapter (or track/episode) title, and the book title with chapter time left. */
@Composable
private fun NowPlayingInfo(
  session: Session?,
  currentMetadata: Metadata,
  currentTime: Duration,
  currentDuration: Duration,
  playbackSpeed: Float,
  hasChapters: Boolean,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier,
  ) {
    if (session != null) {
      CoverImage(
        imageUrl = currentMetadata.artworkUri ?: session.libraryItem.media.coverImageUrl ?: "",
        contentDescription = session.libraryItem.media.metadata.title,
        size = CoverSize,
        shape = RoundedCornerShape(8.dp),
      )
    } else {
      Box(
        modifier = Modifier
          .size(CoverSize)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)),
      )
    }

    Spacer(Modifier.width(12.dp))

    Column(verticalArrangement = Arrangement.Center) {
      Text(
        text = if (session == null) {
          stringResource(Res.string.bottom_bar_nothing_playing)
        } else {
          currentMetadata.title ?: session.title
        },
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        fontFamily = PaytoneOneFontFamily,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )

      val subtitle = when {
        session == null -> stringResource(Res.string.bottom_bar_nothing_playing_hint)
        hasChapters && currentDuration > Duration.ZERO -> {
          val remaining = ((currentDuration - currentTime) / playbackSpeed.toDouble()).coerceAtLeast(Duration.ZERO)
          "${session.libraryItem.media.metadata.title.orEmpty()} · " +
            stringResource(Res.string.bottom_bar_chapter_remaining, remaining.readoutFormat().trim())
        }
        else -> session.libraryItem.media.metadata.title.orEmpty()
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.alpha(0.7f),
      )
    }
  }
}

@Composable
private fun ActionRow(
  enabled: Boolean,
  runningTimer: RunningTimer?,
  onBookmarkAddClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  onTimerClick: () -> Unit,
  showEqualizer: Boolean,
  onEqualizerClick: () -> Unit,
  showChapters: Boolean,
  onChapterListClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.padding(horizontal = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
  ) {
    val bookmarkLabel = stringResource(Res.string.action_add_bookmark)
    IconButtonTooltip(text = bookmarkLabel) {
      IconButton(onClick = onBookmarkAddClick, enabled = enabled) {
        Icon(CampfireIcons.Rounded.Bookmarks, contentDescription = bookmarkLabel)
      }
    }

    speedContent()

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
        val timerLabel = stringResource(Res.string.action_sleep_timer)
        IconButtonTooltip(text = timerLabel) {
          IconButton(onClick = onTimerClick, enabled = enabled) {
            Icon(CampfireIcons.Rounded.Timer, contentDescription = timerLabel)
          }
        }
      } else {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onTimerClick() }
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
              Icon(CampfireIcons.Rounded.Timer, contentDescription = null)
            }
            RunningTimerText(
              runningTimer = timer,
              endOfChapterText = stringResource(Res.string.label_end_of_chapter_short),
              style = {
                when (it) {
                  is PlaybackTimer.EndOfChapter -> MaterialTheme.typography.labelMedium
                  is PlaybackTimer.Epoch -> MaterialTheme.typography.labelMedium
                }
              },
            )
            Spacer(Modifier.width(16.dp))
          }
        }
      }
    }

    if (showEqualizer) {
      val equalizerLabel = stringResource(Res.string.action_equalizer)
      IconButtonTooltip(text = equalizerLabel) {
        IconButton(onClick = onEqualizerClick, enabled = enabled) {
          Icon(CampfireIcons.Rounded.Equalizer, contentDescription = equalizerLabel)
        }
      }
    }

    if (showChapters) {
      val chaptersLabel = stringResource(Res.string.action_chapters)
      IconButtonTooltip(text = chaptersLabel) {
        IconButton(onClick = onChapterListClick, enabled = enabled) {
          Icon(CampfireIcons.Rounded.List, contentDescription = chaptersLabel)
        }
      }
    }
  }
}

private val CoverSize = 48.dp
private val ContentRowHeight = 68.dp
private val TimelineTopPadding = 6.dp
private val TimelineBottomPadding = 4.dp
