// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.playback.expanded

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.windowsizeclass.WindowSizeClass
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.ui.cast.CastButton
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isLandscapePhone
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.Session
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.sessions.ui.composables.PlaybackSpeedAction
import app.campfire.sessions.ui.composables.RunningTimerAction
import app.campfire.sessions.ui.playback.DefaultNonThemedContentColor
import app.campfire.sessions.ui.playback.DefaultNonThemedSheetColor
import app.campfire.sessions.ui.playback.PlaybackUiState
import app.campfire.sessions.ui.playback.PlayerUiEvent
import app.campfire.sessions.ui.playback.PlayerUiState
import app.campfire.sessions.ui.playback.QueueUiEvent
import app.campfire.sessions.ui.playback.QueueUiState
import app.campfire.sessions.ui.playback.SharedBounds
import app.campfire.sessions.ui.playback.SyncUiEvent
import app.campfire.sessions.ui.playback.SyncUiState
import app.campfire.sessions.ui.playback.collapsed.ShadowElevation
import app.campfire.sessions.ui.playback.collapsed.TonalElevation
import app.campfire.sessions.ui.playback.expanded.composables.ActionColumn
import app.campfire.sessions.ui.playback.expanded.composables.AvailableSyncButton
import app.campfire.sessions.ui.playback.expanded.composables.ClearQueueButton
import app.campfire.sessions.ui.playback.expanded.composables.ExpandedItemImage
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackActions
import app.campfire.sessions.ui.playback.expanded.composables.PlaybackSeekBar
import app.campfire.sessions.ui.playback.expanded.composables.QueueButton
import app.campfire.sessions.ui.playback.expanded.composables.QueueContent
import app.campfire.sessions.ui.playback.expanded.composables.TargetSyncContent
import app.campfire.sessions.ui.sheets.bookmarks.BookmarkResult
import app.campfire.sessions.ui.sheets.bookmarks.showBookmarksBottomSheet
import app.campfire.sessions.ui.sheets.chapters.ChapterResult
import app.campfire.sessions.ui.sheets.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.sheets.description.showEpisodeDescriptionBottomSheet
import app.campfire.sessions.ui.sheets.history.PlaybackHistoryResult
import app.campfire.sessions.ui.sheets.history.showPlaybackHistoryBottomSheet
import app.campfire.sessions.ui.sheets.sleeptimer.TimerResult
import app.campfire.sessions.ui.sheets.sleeptimer.showSleepTimerBottomSheet
import app.campfire.sessions.ui.sheets.speed.showPlaybackSpeedBottomSheet
import app.campfire.sessions.ui.sheets.tracks.AudioTrackResult
import app.campfire.sessions.ui.sheets.tracks.showAudioTrackBottomSheet
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.action_close
import campfire.features.sessions.ui.generated.resources.misaligned_chapters_error_message
import campfire.features.sessions.ui.generated.resources.playback_error_message
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
internal fun <T> T.SmallExpandedPlaybackBar(
  navigator: Navigator,
  session: Session?,
  playbackState: PlaybackUiState,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
  containerColor: Color = DefaultNonThemedSheetColor,
  contentColor: Color = DefaultNonThemedContentColor,
) where T : SharedTransitionScope, T : AnimatedVisibilityScope {
  val overlayHost = rememberOverlayHost()
  ContentWithOverlays(
    overlayHost = overlayHost,
    modifier = modifier,
  ) {
    SmallExpandedPlaybackBar(
      containerColor = containerColor,
      contentColor = contentColor,
      navigator = navigator,
      overlayHost = overlayHost,
      session = session,
      itemValidation = playbackState.validation,
      playerState = playbackState.playerState,
      queueState = playbackState.queueState,
      syncState = playbackState.syncUiState,
      playbackHistoryEnabled = playbackState.playbackHistoryEnabled,
      onClose = onClose,
      sharedTransitionScope = this,
      animatedVisibilityScope = this,
    )
  }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SmallExpandedPlaybackBar(
  navigator: Navigator,
  overlayHost: OverlayHost,

  session: Session?,
  itemValidation: LibraryItemValidation,
  playerState: PlayerUiState,
  queueState: QueueUiState,
  syncState: SyncUiState,
  playbackHistoryEnabled: Boolean,

  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  containerColor: Color = DefaultNonThemedSheetColor,
  contentColor: Color = DefaultNonThemedContentColor,
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
      .fluentIf(windowSizeClass.isSupportingPaneEnabled && !windowSizeClass.isLandscapePhone) {
        systemBarsPadding()
          .padding(top = 32.dp)
      }
      .fluentIf(windowSizeClass.isLandscapePhone) {
        systemBarsPadding()
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
      var showQueue by remember { mutableStateOf(false) }
      val hasQueue = queueState.queue.isNotEmpty()

      TopAppBar(
        title = {
          androidx.compose.animation.AnimatedVisibility(
            visible = hasQueue,
            enter = fadeIn(),
            exit = fadeOut(),
          ) {
            QueueButton(
              checked = showQueue,
              onCheckedChange = { showQueue = it },
              buttonSize = ButtonDefaults.ExtraSmallContainerHeight,
            )
          }
        },
        navigationIcon = {
          val closeLabel = stringResource(Res.string.action_close)
          IconButtonTooltip(text = closeLabel) {
            IconButton(
              onClick = onClose,
            ) {
              Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = closeLabel)
            }
          }
        },
        actions = {
          AnimatedContent(
            targetState = hasQueue && showQueue,
          ) { isQueueVisible ->
            if (isQueueVisible) {
              Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp),
              ) {
                ClearQueueButton(
                  onConfirmClick = {
                    queueState.eventSink(QueueUiEvent.ClearQueue)
                  },
                )
              }
            } else {
              CastButton()
            }
          }
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

      AnimatedContent(
        targetState = showQueue && hasQueue,
        modifier = Modifier.weight(1f),
      ) { queue ->
        if (queue) {
          QueueContent(
            queue = queueState.queue,
            onItemClick = { entry ->
              queueState.eventSink(QueueUiEvent.QueueItemClick(entry))
              showQueue = false
            },
            onRemoveItem = { entry ->
              queueState.eventSink(QueueUiEvent.RemoveQueueItem(entry))
            },
            onReorderItem = { fromKey, toKey ->
              queueState.eventSink(QueueUiEvent.ReorderItem(fromKey, toKey))
            },
            onReorderStopped = {
              queueState.eventSink(QueueUiEvent.ReorderStopped)
            },
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          SmallExpandedPlaybackContent(
            navigator = navigator,
            overlayHost = overlayHost,
            session = session,
            playerState = playerState,
            syncState = syncState,
            itemValidation = itemValidation,
            playbackHistoryEnabled = playbackHistoryEnabled,
            onClose = onClose,
            windowSizeClass = windowSizeClass,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun SharedTransitionScope.SmallExpandedPlaybackContent(
  navigator: Navigator,
  overlayHost: OverlayHost,

  session: Session?,
  playerState: PlayerUiState,
  syncState: SyncUiState,
  itemValidation: LibraryItemValidation,
  playbackHistoryEnabled: Boolean,

  onClose: () -> Unit,

  windowSizeClass: WindowSizeClass,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val isDragged by interactionSource.collectIsDraggedAsState()
  val isInteracting = isPressed || isDragged

  Column(
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
    ) {
      this@SmallExpandedPlaybackContent.ItemMetadata(
        navigator = navigator,
        playerState = playerState,
        session = session,
        itemValidation = itemValidation,
        animatedVisibilityScope = animatedVisibilityScope,
        onClose = onClose,
        modifier = Modifier
          .fillMaxHeight()
          .weight(0.8f),
      )

      ItemActions(
        session = session,
        playerState = playerState,
        syncState = syncState,
        isInteracting = isInteracting,
        interactionSource = interactionSource,
        modifier = Modifier
          .fillMaxHeight()
          .weight(1.2f),
      )

      PlaybackOptionsColumn(
        scope = scope,
        overlayHost = overlayHost,
        session = session,
        playerState = playerState,
        playbackHistoryEnabled = playbackHistoryEnabled,
        modifier = Modifier.padding(bottom = 16.dp),
      )
    }
  }
}

@Composable
private fun SharedTransitionScope.ItemMetadata(
  navigator: Navigator,
  playerState: PlayerUiState,
  session: Session?,
  itemValidation: LibraryItemValidation,
  animatedVisibilityScope: AnimatedVisibilityScope,
  onClose: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
  ) {
    ExpandedItemImage(
      currentMetadata = playerState.metadata,
      runningTimer = playerState.timer,
      session = session,
      animatedVisibilityScope = animatedVisibilityScope,
      size = Dp.Unspecified,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 16.dp)
        .aspectRatio(1f)
        .clickable {
          if (session == null) return@clickable
          scope.launch {
            onClose()
            delay(350.milliseconds)
            navigator.goTo(
              LibraryItemScreen(
                libraryItemId = session.libraryItem.id,
                episodeId = session.episodeId,
              ),
            )
          }
        },
    )

    Spacer(Modifier.height(16.dp))

    Text(
      text = playerState.metadata.title ?: session?.title ?: Session.TITLE_PLACEHOLDER,
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.headlineSmall,
      fontWeight = FontWeight.SemiBold,
      fontFamily = PaytoneOneFontFamily,
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(horizontal = 24.dp),
    )

    Text(
      text = session?.libraryItem?.media?.metadata?.title ?: "",
      textAlign = TextAlign.Center,
      style = MaterialTheme.typography.titleSmall,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier
        .align(Alignment.CenterHorizontally)
        .padding(horizontal = 24.dp)
        .alpha(50f),
    )

    if (itemValidation is LibraryItemValidation.Error.InvalidChapters) {
      Spacer(Modifier.height(4.dp))
      Text(
        text = stringResource(Res.string.misaligned_chapters_error_message),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        fontStyle = FontStyle.Italic,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(horizontal = 64.dp),
      )
    }

    Spacer(Modifier.height(16.dp))
  }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ItemActions(
  session: Session?,
  playerState: PlayerUiState,
  syncState: SyncUiState,
  isInteracting: Boolean,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
  ) {
    if (playerState.error != null) {
      Text(
        text = stringResource(Res.string.playback_error_message),
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        fontStyle = FontStyle.Italic,
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(horizontal = 64.dp),
      )
      Spacer(Modifier.height(4.dp))
    }

    syncState.availableSync?.let { sync ->
      AvailableSyncButton(
        currentTime = sync.currentTime,
        targetTime = sync.targetTime,
        syncTimeInMillis = sync.syncTimeInMillis,
        targetContent = {
          sync.targetChapterTitle?.let { title ->
            TargetSyncContent(title)
          }
        },
        onClick = {
          syncState.eventSink(SyncUiEvent.Sync)
        },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .padding(
            horizontal = 16.dp,
            vertical = 8.dp,
          ),
      )
    }

    PlaybackActions(
      state = playerState.state,
      isInteracting = isInteracting,
      buttonSize = 72.dp,
      buttonShape = MaterialTheme.shapes.largeIncreased,
      playButtonExtraWidth = 20.dp,
      onSkipPreviousClick = {
        playerState.eventSink(PlayerUiEvent.PreviousClick)
      },
      onRewindClick = {
        playerState.eventSink(PlayerUiEvent.RewindClick)
      },
      onPlayPauseClick = {
        playerState.eventSink(PlayerUiEvent.PlayPauseClick)
      },
      onForwardClick = {
        playerState.eventSink(PlayerUiEvent.FastForwardClick)
      },
      onSkipNextClick = {
        playerState.eventSink(PlayerUiEvent.NextClick)
      },
    )

    Spacer(Modifier.height(8.dp))

    if (playerState.bookTimeEnabled && session?.episodeId == null) {
      BookTimeProgressIndicator(session, playerState)
      Spacer(Modifier.height(4.dp))
    }

    PlaybackSeekBar(
      state = playerState.state,
      currentTime = playerState.time,
      currentDuration = playerState.duration,
      playbackSpeed = playerState.speed,
      onSeek = { percent ->
        playerState.eventSink(PlayerUiEvent.Seek.Percent(percent))
      },
      interactionSource = interactionSource,
    )
  }
}

@Composable
private fun PlaybackOptionsColumn(
  scope: CoroutineScope,
  overlayHost: OverlayHost,
  session: Session?,
  playerState: PlayerUiState,
  playbackHistoryEnabled: Boolean,
  modifier: Modifier = Modifier,
) {
  ActionColumn(
    onBookmarksClick = {
      scope.launch {
        when (val result = overlayHost.showBookmarksBottomSheet(session!!.libraryItem.id)) {
          is BookmarkResult.Selected -> {
            playerState.eventSink(PlayerUiEvent.BookmarkSelected(result.bookmark))
          }

          BookmarkResult.None -> Unit
        }
      }
    },
    speedContent = {
      PlaybackSpeedAction(
        playbackSpeed = playerState.speed,
        onClick = {
          scope.launch {
            overlayHost.showPlaybackSpeedBottomSheet(session!!.libraryItem.id, playerState.speed)
          }
        },
      )
    },
    timerContent = {
      RunningTimerAction(
        runningTimer = playerState.timer,
        currentTime = playerState.time,
        currentDuration = playerState.duration,
        playbackSpeed = playerState.speed,
        onClick = {
          scope.launch {
            when (val result = overlayHost.showSleepTimerBottomSheet(playerState.timer)) {
              is TimerResult.Selected -> {
                playerState.eventSink(PlayerUiEvent.TimerSelected(result.timer))
              }

              TimerResult.Cleared -> {
                playerState.eventSink(PlayerUiEvent.ClearTimer)
              }

              else -> Unit
            }
          }
        },
      )
    },
    onChapterListClick = {
      if (session!!.libraryItem.media.chapters.isNotEmpty()) {
        scope.launch {
          val result = overlayHost.showChapterBottomSheet(
            chapters = session.libraryItem.media.chapters,
            currentChapter = session.chapter,
            playbackSpeed = playerState.speed,
          )
          if (result is ChapterResult.Selected) {
            playerState.eventSink(PlayerUiEvent.ChapterSelected(result.chapter))
          }
        }
      } else if (session.libraryItem.media.tracks.isNotEmpty()) {
        scope.launch {
          val result = overlayHost.showAudioTrackBottomSheet(
            audioTracks = session.libraryItem.media.tracks,
            currentAudioTrack = session.audioTrack,
            playbackSpeed = playerState.speed,
          )
          if (result is AudioTrackResult.Selected) {
            playerState.eventSink(PlayerUiEvent.AudioTrackSelected(result.audioTrack))
          }
        }
      }
    },
    showChapters = session?.episodeId == null,
    onDescriptionClick = {
      val episode = session?.episode ?: return@ActionColumn
      scope.launch {
        overlayHost.showEpisodeDescriptionBottomSheet(
          episode = episode,
          onSeek = { duration ->
            playerState.eventSink(PlayerUiEvent.Seek.Position(duration))
          },
        )
      }
    },
    showDescription = session?.episodeId != null,
    onHistoryClick = {
      scope.launch {
        val result = overlayHost.showPlaybackHistoryBottomSheet(session!!.libraryItem.id)
        if (result is PlaybackHistoryResult.Selected) {
          val position = result.action.toPosition ?: result.action.fromPosition
          playerState.eventSink(PlayerUiEvent.Seek.Position(position))
        }
      }
    },
    showHistory = playbackHistoryEnabled,
    modifier = modifier,
  )
}
