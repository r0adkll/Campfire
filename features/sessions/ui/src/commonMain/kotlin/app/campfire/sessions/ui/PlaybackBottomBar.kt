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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.EqualizerState
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.compose.extensions.readoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Bookmarks
import app.campfire.common.compose.icons.rounded.DockToBottom
import app.campfire.common.compose.icons.rounded.Equalizer
import app.campfire.common.compose.icons.rounded.List
import app.campfire.common.compose.icons.rounded.MoreVert
import app.campfire.common.compose.icons.rounded.OpenInNew
import app.campfire.common.compose.icons.rounded.Timer
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.theme.colorScheme
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
import app.campfire.sessions.ui.composables.OutputDeviceControl
import app.campfire.sessions.ui.composables.OutputDeviceMenuItems
import app.campfire.sessions.ui.composables.PlaybackSpeedAction
import app.campfire.sessions.ui.composables.RunningTimerText
import app.campfire.sessions.ui.composables.VolumeControl
import app.campfire.sessions.ui.playback.DefaultNonThemedContentColor
import app.campfire.sessions.ui.playback.DefaultNonThemedSheetColor
import app.campfire.sessions.ui.playback.DefaultSheetColor
import app.campfire.sessions.ui.playback.DefaultSheetContentColor
import app.campfire.sessions.ui.playback.OutputDeviceUiState
import app.campfire.sessions.ui.playback.PlaybackPresenterFactory
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.PlayerUiEvent
import app.campfire.sessions.ui.playback.VolumeUiState
import app.campfire.sessions.ui.player.LocalMiniPlayerHost
import app.campfire.sessions.ui.player.MiniPlayerAction
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
import campfire.features.sessions.ui.generated.resources.action_more
import campfire.features.sessions.ui.generated.resources.action_open_mini_player
import campfire.features.sessions.ui.generated.resources.action_output_device
import campfire.features.sessions.ui.generated.resources.action_return_to_window
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

/**
 * Re-themes the bar to the current item's cover palette, the same way the floating collapsed and
 * expanded bars do, falling back to the app's own scheme when dynamic playback theming is off or
 * nothing is playing.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaybackBottomBar(
  uiState: PlaybackUiState,
  modifier: Modifier = Modifier,
) {
  val playerState = uiState.playerState
  val miniPlayerHost = LocalMiniPlayerHost.current
  MaterialExpressiveTheme(
    colorScheme = uiState.themeState.theme?.colorScheme,
  ) {
    PlaybackBottomBarContent(
      session = uiState.session,
      containerColor = if (uiState.themeState.dynamicThemingEnabled) {
        DefaultSheetColor
      } else {
        DefaultNonThemedSheetColor
      },
      contentColor = if (uiState.themeState.dynamicThemingEnabled) {
        DefaultSheetContentColor
      } else {
        DefaultNonThemedContentColor
      },
      miniPlayerOpen = miniPlayerHost?.isOpen,
      onMiniPlayerClick = {
        if (miniPlayerHost == null) return@PlaybackBottomBarContent
        if (miniPlayerHost.isOpen) miniPlayerHost.close() else miniPlayerHost.open()
      },
      state = playerState.state,
      playbackSpeed = playerState.speed,
      currentTime = playerState.time,
      currentDuration = playerState.duration,
      bookTime = playerState.bookTime,
      currentMetadata = playerState.metadata,
      runningTimer = playerState.timer,
      equalizer = playerState.equalizer,
      bookmarks = playerState.bookmarks,
      volume = uiState.volume,
      outputDevices = uiState.outputDevices,
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
  /** Null on platforms with no app-level volume, where the control is not rendered at all. */
  volume: VolumeUiState?,
  /** Null where output cannot be routed to a chosen device. */
  outputDevices: OutputDeviceUiState?,

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
  containerColor: Color = DefaultNonThemedSheetColor,
  contentColor: Color = DefaultNonThemedContentColor,
  /** Whether the mini-player window is open, or null where the platform has no such window. */
  miniPlayerOpen: Boolean? = null,
  onMiniPlayerClick: () -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  val hasSession = session != null
  val chapters = session?.libraryItem?.media?.chapters.orEmpty()
  val hasChapters = chapters.isNotEmpty() && session?.episodeId == null

  Surface(
    color = containerColor,
    contentColor = contentColor,
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
          volume = volume,
          outputDevices = outputDevices,
          miniPlayerOpen = miniPlayerOpen,
          onMiniPlayerClick = onMiniPlayerClick,
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

/**
 * The tools at the right of the bar, collapsing into an overflow menu as space runs out.
 *
 * The row genuinely cannot show everything at the 840dp breakpoint where this bar first appears:
 * the flexible space either side of the fixed transport block is 568dp, an even split gives this
 * side 284dp, and the full set wants ~372dp. Widening this side instead would push the transport
 * off centre by more the wider the window got, so the actions give way rather than the layout.
 *
 * Tiers are driven by measured width against the worst case for each item — the speed control is
 * an icon at 1x and a wider "1.25x" label otherwise, and the sleep timer grows a countdown pill
 * while running, so both are budgeted at their larger form.
 */
@Composable
private fun ActionRow(
  enabled: Boolean,
  runningTimer: RunningTimer?,
  volume: VolumeUiState?,
  outputDevices: OutputDeviceUiState?,
  onBookmarkAddClick: () -> Unit,
  speedContent: @Composable () -> Unit,
  onTimerClick: () -> Unit,
  showEqualizer: Boolean,
  onEqualizerClick: () -> Unit,
  showChapters: Boolean,
  onChapterListClick: () -> Unit,
  miniPlayerOpen: Boolean?,
  onMiniPlayerClick: () -> Unit,
  modifier: Modifier = Modifier,
) = BoxWithConstraints(modifier) {
  val equalizerLabel = stringResource(Res.string.action_equalizer)
  val chaptersLabel = stringResource(Res.string.action_chapters)
  val timerLabel = stringResource(Res.string.action_sleep_timer)

  val overflowed = actionOverflow(
    available = maxWidth,
    hasEqualizer = showEqualizer,
    hasChapters = showChapters,
    hasOutputDevices = outputDevices != null,
    timerRunning = runningTimer != null,
    hasMiniPlayer = miniPlayerOpen != null,
  )

  Row(
    // Fills the measured box rather than wrapping, so Alignment.End still packs the tools
    // against the bar's trailing edge
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp),
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
      targetState = runningTimer.takeIf { OverflowAction.Timer !in overflowed },
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
      if (OverflowAction.Timer in overflowed) {
        Unit
      } else if (timer == null) {
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

    if (showEqualizer && OverflowAction.Equalizer !in overflowed) {
      IconButtonTooltip(text = equalizerLabel) {
        IconButton(onClick = onEqualizerClick, enabled = enabled) {
          Icon(CampfireIcons.Rounded.Equalizer, contentDescription = equalizerLabel)
        }
      }
    }

    if (showChapters && OverflowAction.Chapters !in overflowed) {
      IconButtonTooltip(text = chaptersLabel) {
        IconButton(onClick = onChapterListClick, enabled = enabled) {
          Icon(CampfireIcons.Rounded.List, contentDescription = chaptersLabel)
        }
      }
    }

    if (outputDevices != null && OverflowAction.OutputDevice !in overflowed) {
      OutputDeviceControl(state = outputDevices)
    }

    volume?.let { VolumeControl(state = it) }

    // The mini window carries its own return button, so this stays usable once the player is
    // out there but is never essential — it folds with the first tier.
    if (miniPlayerOpen != null && OverflowAction.MiniPlayer !in overflowed) {
      MiniPlayerAction(
        isOpen = miniPlayerOpen,
        onClick = onMiniPlayerClick,
        enabled = enabled || miniPlayerOpen,
      )
    }

    if (overflowed.isNotEmpty()) {
      ActionOverflowMenu(
        overflowed = overflowed,
        enabled = enabled || miniPlayerOpen == true,
        runningTimer = runningTimer,
        outputDevices = outputDevices,
        miniPlayerOpen = miniPlayerOpen,
        equalizerLabel = equalizerLabel,
        chaptersLabel = chaptersLabel,
        timerLabel = timerLabel,
        onEqualizerClick = onEqualizerClick,
        onChapterListClick = onChapterListClick,
        onTimerClick = onTimerClick,
        onMiniPlayerClick = onMiniPlayerClick,
      )
    }
  }
}

/** An action that can be folded away when the bar's tool row runs out of room. */
internal enum class OverflowAction { MiniPlayer, OutputDevice, Equalizer, Chapters, Timer }

/**
 * Which actions must fold away for the row to fit in [available].
 *
 * Collapses in least-useful-first order: the mini-player toggle, output device, equalizer and
 * chapter list go together, and the sleep timer only follows when even that is not enough — which
 * happens at the narrowest docked widths once the speed control is showing a label rather than
 * its 1x icon.
 */
internal fun actionOverflow(
  available: Dp,
  hasEqualizer: Boolean,
  hasChapters: Boolean,
  hasOutputDevices: Boolean = false,
  timerRunning: Boolean,
  hasMiniPlayer: Boolean = false,
): Set<OverflowAction> {
  val timerSize = if (timerRunning) RunningTimerSize else ActionSize
  val optional = buildList {
    if (hasMiniPlayer) add(OverflowAction.MiniPlayer)
    if (hasOutputDevices) add(OverflowAction.OutputDevice)
    if (hasEqualizer) add(OverflowAction.Equalizer)
    if (hasChapters) add(OverflowAction.Chapters)
  }
  // Always on screen: bookmark, speed, volume — plus the overflow button once anything folds.
  val fixed = ActionSize + WidestSpeedSize + ActionSize
  val fullWidth = fixed + (ActionSize * optional.size) + timerSize +
    actionSpacing(optional.size + 3)
  if (available >= fullWidth) return emptySet()

  val withoutOptional = fixed + timerSize + ActionSize + actionSpacing(4)
  if (available >= withoutOptional) return optional.toSet()

  return optional.toSet() + OverflowAction.Timer
}

private fun actionSpacing(items: Int): Dp = ActionSpacing * (items - 1).coerceAtLeast(0) + RowPadding * 2

/**
 * The folded-away actions, behind a single button. Uses the same trailing position the actions
 * themselves occupied, so nothing jumps as the window crosses a tier.
 */
@Composable
private fun ActionOverflowMenu(
  overflowed: Set<OverflowAction>,
  enabled: Boolean,
  runningTimer: RunningTimer?,
  outputDevices: OutputDeviceUiState?,
  miniPlayerOpen: Boolean?,
  equalizerLabel: String,
  chaptersLabel: String,
  timerLabel: String,
  onEqualizerClick: () -> Unit,
  onChapterListClick: () -> Unit,
  onTimerClick: () -> Unit,
  onMiniPlayerClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by remember { mutableStateOf(false) }
  val moreLabel = stringResource(Res.string.action_more)

  Box(modifier) {
    IconButtonTooltip(text = moreLabel) {
      IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(CampfireIcons.Rounded.MoreVert, contentDescription = moreLabel)
      }
    }

    DropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
      shape = MaterialTheme.shapes.medium,
    ) {
      ActionOverflowMenuItems(
        overflowed = overflowed,
        runningTimer = runningTimer,
        outputDevices = outputDevices,
        equalizerLabel = equalizerLabel,
        chaptersLabel = chaptersLabel,
        timerLabel = timerLabel,
        onEqualizerClick = onEqualizerClick,
        onChapterListClick = onChapterListClick,
        onTimerClick = onTimerClick,
        onChosen = { expanded = false },
        miniPlayerOpen = miniPlayerOpen,
        onMiniPlayerClick = onMiniPlayerClick,
      )
    }
  }
}

/**
 * What the overflow button offers, split out from the menu itself so it can be rendered directly
 * in tests — an [androidx.compose.ui.ImageComposeScene] draws the main scene only, and a
 * `DropdownMenu`'s content lives in a popup layer it never captures.
 */
@Composable
internal fun ActionOverflowMenuItems(
  overflowed: Set<OverflowAction>,
  runningTimer: RunningTimer?,
  outputDevices: OutputDeviceUiState?,
  equalizerLabel: String,
  chaptersLabel: String,
  timerLabel: String,
  onEqualizerClick: () -> Unit,
  onChapterListClick: () -> Unit,
  onTimerClick: () -> Unit,
  onChosen: () -> Unit,
  /** Whether the mini-player window is open, or null where the platform has no such window. */
  miniPlayerOpen: Boolean? = null,
  onMiniPlayerClick: () -> Unit = {},
) {
  // The device picker folds away first, so this is the usual route to it. Its own rows are
  // reused rather than a menu nested inside a menu item.
  if (outputDevices != null && OverflowAction.OutputDevice in overflowed) {
    Text(
      text = stringResource(Res.string.action_output_device),
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
    )
    OutputDeviceMenuItems(outputDevices, onChosen = onChosen)
    HorizontalDivider()
  }

  if (OverflowAction.Timer in overflowed) {
    DropdownMenuItem(
      text = { Text(timerLabel) },
      leadingIcon = { Icon(CampfireIcons.Rounded.Timer, contentDescription = null) },
      trailingIcon = runningTimer?.let {
        {
          RunningTimerText(
            runningTimer = it,
            endOfChapterText = stringResource(Res.string.label_end_of_chapter_short),
            style = { MaterialTheme.typography.labelMedium },
          )
        }
      },
      onClick = {
        onChosen()
        onTimerClick()
      },
    )
  }

  if (OverflowAction.Equalizer in overflowed) {
    DropdownMenuItem(
      text = { Text(equalizerLabel) },
      leadingIcon = { Icon(CampfireIcons.Rounded.Equalizer, contentDescription = null) },
      onClick = {
        onChosen()
        onEqualizerClick()
      },
    )
  }

  if (OverflowAction.Chapters in overflowed) {
    DropdownMenuItem(
      text = { Text(chaptersLabel) },
      leadingIcon = { Icon(CampfireIcons.Rounded.List, contentDescription = null) },
      onClick = {
        onChosen()
        onChapterListClick()
      },
    )
  }

  if (miniPlayerOpen != null && OverflowAction.MiniPlayer in overflowed) {
    DropdownMenuItem(
      text = {
        Text(
          stringResource(
            if (miniPlayerOpen) Res.string.action_return_to_window else Res.string.action_open_mini_player,
          ),
        )
      },
      leadingIcon = {
        Icon(
          imageVector = if (miniPlayerOpen) CampfireIcons.Rounded.DockToBottom else CampfireIcons.Rounded.OpenInNew,
          contentDescription = null,
        )
      },
      onClick = {
        onChosen()
        onMiniPlayerClick()
      },
    )
  }
}

/** A standard icon button's footprint, and the widest each variable-width control gets. */
private val ActionSize = 48.dp
private val WidestSpeedSize = 96.dp
private val RunningTimerSize = 136.dp
private val ActionSpacing = 4.dp
private val RowPadding = 8.dp

private val CoverSize = 48.dp
private val ContentRowHeight = 68.dp
private val TimelineTopPadding = 6.dp
private val TimelineBottomPadding = 4.dp
