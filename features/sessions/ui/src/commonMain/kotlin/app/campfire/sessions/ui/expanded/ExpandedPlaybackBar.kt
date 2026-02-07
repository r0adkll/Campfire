package app.campfire.sessions.ui.expanded

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.audioplayer.ui.TimerResult
import app.campfire.audioplayer.ui.cast.CastButton
import app.campfire.audioplayer.ui.showTimerBottomSheet
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.di.rememberComponent
import app.campfire.common.compose.layout.isSupportingPaneEnabled
import app.campfire.common.compose.theme.PaytoneOneFontFamily
import app.campfire.common.compose.widgets.CoverImageSize
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.fluentIf
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import app.campfire.libraries.api.screen.LibraryItemScreen
import app.campfire.sessions.ui.FlingThreshold
import app.campfire.sessions.ui.ShadowElevation
import app.campfire.sessions.ui.SharedBounds
import app.campfire.sessions.ui.TonalElevation
import app.campfire.sessions.ui.TranslationThreshold
import app.campfire.sessions.ui.composables.PlaybackSpeedAction
import app.campfire.sessions.ui.composables.RunningTimerAction
import app.campfire.sessions.ui.expanded.composables.ActionRow
import app.campfire.sessions.ui.expanded.composables.ExpandedItemImage
import app.campfire.sessions.ui.expanded.composables.PlaybackActions
import app.campfire.sessions.ui.expanded.composables.PlaybackSeekBar
import app.campfire.sessions.ui.expanded.composables.QueueButton
import app.campfire.sessions.ui.expanded.composables.QueueItem
import app.campfire.sessions.ui.sheets.bookmarks.BookmarkResult
import app.campfire.sessions.ui.sheets.bookmarks.showBookmarksBottomSheet
import app.campfire.sessions.ui.sheets.chapters.ChapterResult
import app.campfire.sessions.ui.sheets.chapters.showChapterBottomSheet
import app.campfire.sessions.ui.sheets.speed.showPlaybackSpeedBottomSheet
import app.campfire.sessions.ui.sheets.tracks.AudioTrackResult
import app.campfire.sessions.ui.sheets.tracks.showAudioTrackBottomSheet
import campfire.features.sessions.ui.generated.resources.Res
import campfire.features.sessions.ui.generated.resources.queue_header_queue
import campfire.features.sessions.ui.generated.resources.queue_header_up_next
import com.r0adkll.kimchi.annotations.ContributesTo
import com.slack.circuit.overlay.ContentWithOverlays
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.rememberOverlayHost
import com.slack.circuit.runtime.Navigator
import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

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
  onAudioTrackSelected: (AudioTrack) -> Unit,
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
      onAudioTrackSelected = onAudioTrackSelected,
      onBookmarkSelected = onBookmarkSelected,
      onClose = onClose,
      sharedTransitionScope = sharedTransitionScope,
      animatedVisibilityScope = animatedVisibilityScope,
    )
  }
}

@ContributesTo(UserScope::class)
interface ExpandedPlaybackBarComponent {
  val expandedPlaybackPresenterFactory: ExpandedPlaybackPresenterFactory
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3ExpressiveApi::class)
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
  onAudioTrackSelected: (AudioTrack) -> Unit,
  onBookmarkSelected: (Bookmark) -> Unit,

  onClose: () -> Unit,
  sharedTransitionScope: SharedTransitionScope,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
  component: ExpandedPlaybackBarComponent = rememberComponent(),
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

  // Presentation / ViewState
  val presenter = remember { component.expandedPlaybackPresenterFactory() }
  val viewState = presenter.present()

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
      var showQueue by remember { mutableStateOf(false) }
      val hasQueue = viewState.queue.isNotEmpty()

      TopAppBar(
        title = {
          AnimatedVisibility(
            visible = hasQueue,
          ) {
            QueueButton(
              checked = showQueue,
              onCheckedChange = { showQueue = it },
              buttonSize = ButtonDefaults.MinHeight,
            )
          }
        },
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

      AnimatedContent(
        targetState = showQueue && hasQueue,
        modifier = Modifier.weight(1f),
      ) { queue ->
        if (queue) {
          QueueContent(
            queue = viewState.queue,
            onItemClick = { item ->
              viewState.eventSink(ExpandedPlaybackUiEvent.QueueItemClick(item))
              showQueue = false
            },
            onRemoveItem = { item ->
              viewState.eventSink(ExpandedPlaybackUiEvent.RemoveQueueItem(item))
            },
            onReorderItem = { from, to ->
              viewState.reorderSink(from, to)
            },
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          ExpandedPlaybackContent(
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
            onTimerSelected = onTimerSelected,
            onTimerCleared = onTimerCleared,
            onChapterSelected = onChapterSelected,
            onAudioTrackSelected = onAudioTrackSelected,
            onBookmarkSelected = onBookmarkSelected,
            onClose = onClose,
            windowSizeClass = windowSizeClass,
            animatedVisibilityScope = animatedVisibilityScope,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }

      Spacer(Modifier.navigationBarsPadding())
    }
  }
}

@Composable
private fun QueueContent(
  queue: List<LibraryItem>,
  onItemClick: (LibraryItem) -> Unit,
  onRemoveItem: (LibraryItem) -> Unit,
  onReorderItem: suspend (from: LibraryItemId, to: LibraryItemId) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val lazyListState = rememberLazyListState()
  val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
    onReorderItem(from.key as LibraryItemId, to.key as LibraryItemId)
    haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
  }
  LazyColumn(
    modifier = modifier,
    state = lazyListState,
    verticalArrangement = Arrangement.spacedBy(8.dp),
    contentPadding = PaddingValues(
      horizontal = 16.dp,
    ),
  ) {
    queue.groupBy { queue.indexOf(it) == 0 }.forEach { (isFirst, items) ->
      if (isFirst) {
        item {
          MetadataHeader(
            title = stringResource(Res.string.queue_header_up_next),
          )
        }
      } else {
        item {
          MetadataHeader(
            title = stringResource(Res.string.queue_header_queue),
          )
        }
      }

      items(
        items = items,
        key = { it.id },
      ) { item ->
        ReorderableItem(reorderableLazyListState, key = item.id) {
          val interactionSource = remember { MutableInteractionSource() }
          QueueItem(
            item = item,
            onClick = { onItemClick(item) },
            onRemove = { onRemoveItem(item) },
            interactionSource = interactionSource,
            modifier = Modifier
              .animateItem()
              .longPressDraggableHandle(
                onDragStarted = {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                },
                onDragStopped = {
                  haptics.performHapticFeedback(HapticFeedbackType.GestureEnd)
                },
                interactionSource = interactionSource,
              ),
          )
        }
      }
    }
  }
}

@Composable
private fun SharedTransitionScope.ExpandedPlaybackContent(
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
  onAudioTrackSelected: (AudioTrack) -> Unit,
  onBookmarkSelected: (Bookmark) -> Unit,
  onClose: () -> Unit,

  windowSizeClass: WindowSizeClass,
  animatedVisibilityScope: AnimatedVisibilityScope,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()

  Column(
    modifier = modifier,
  ) {
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
          maxLines = 3,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(horizontal = 24.dp),
        )

        Text(
          text = session.libraryItem.media.metadata.title ?: "",
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
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
