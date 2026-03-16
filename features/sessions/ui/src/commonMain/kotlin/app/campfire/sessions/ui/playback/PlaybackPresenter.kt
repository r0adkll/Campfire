package app.campfire.sessions.ui.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.PlaybackController
import app.campfire.audioplayer.model.Metadata
import app.campfire.core.extensions.asDateTime
import app.campfire.core.extensions.epochMilliseconds
import app.campfire.core.extensions.readableFormat
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Corked
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.user.api.MediaProgressRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import me.tatarka.inject.annotations.Inject

typealias PlaybackPresenterFactory = () -> PlaybackPresenter

@Inject
class PlaybackPresenter(
  private val sessionQueue: SessionQueue,
  private val sessionsRepository: SessionsRepository,
  private val libraryItemValidator: LibraryItemValidator,
  private val mediaProgressRepository: MediaProgressRepository,
  private val playbackController: PlaybackController,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val themeSettings: ThemeSettings,
  private val themeManager: ThemeManager,
) : Corked("PlaybackPresenter") {

  @Composable
  fun present(expanded: Boolean): PlaybackUiState {
    val scope = rememberCoroutineScope()

    // Check if we should initializer the playback session
    CheckInitializePlayerSession()

    val currentSession by remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val playerState = observePlayerState(currentSession)
    val queueState = observeQueueState()
    val syncState = observeSyncState(currentSession, expanded)
    val themeState = observeThemeState(currentSession)
    val itemValidation = observeItemValidation(currentSession)

    return PlaybackUiState(
      session = currentSession,
      playerState = playerState,
      queueState = queueState,
      themeState = themeState,
      syncUiState = syncState,
      validation = itemValidation,
    ) { event ->
      when (event) {
        PlaybackUiEvent.ClearSession -> {
          currentSession?.let { s ->
            scope.launch {
              playbackController.stopSession(s.libraryItem.id, clearQueue = true)
            }
          }
        }

        PlaybackUiEvent.StartSession -> {
          currentSession?.let { s ->
            scope.launch {
              playbackController.startSession(s.libraryItem.id)
            }
          }
        }
      }
    }
  }

  @Composable
  private fun CheckInitializePlayerSession() {
    // Launch ONLY when first initialized. Since this presenter is retained in the composition
    // this SHOULD only fire once.
    LaunchedEffect(Unit) {
      ibark { "~~> Starting playback controller initialization" }
      val currentSession = sessionsRepository.getCurrentSession()
      if (currentSession != null) {
        ibark { "<~~ Found current session: ${currentSession.libraryItem.id.loggableId}" }
        // Okay, we have a valid session which means we should prime the playback, when available
        // and if not already initialized
        val player = audioPlayerHolder.currentPlayer.value
        val playerSessionId = player?.preparedSession?.id
        val playerState = player?.state?.value
        if (
          player == null ||
          playerSessionId != currentSession.id ||
          playerState == AudioPlayer.State.Disabled
        ) {
          dbark { "<~~ Player not initialized [${playerSessionId}, $playerState]" }

          // Since we want to auto-sync listening session progress let's make sure to refresh the current
          // sessions libraryItem media progress, if possible.
          dbark { "~~> Refreshing media progress…" }
          val (progress, duration) = measureTimedValue {
            // Since this is a network operation we want to timebox refreshing the current progress
            // So we don't create an awkward delay when initialing the playback session for the UI
            withTimeoutOrNull(1.seconds) {
              mediaProgressRepository.getProgress(currentSession.libraryItem.id, fresh = true)
            }
          }
          dbark {
            "<~~ Refreshed media progress in $duration [${progress?.actualTime} @ " +
              "${progress?.lastUpdate?.asDateTime()?.readableFormat}]"
          }

          ibark { "--> Starting playback controller initialization" }
          // Okay either the player is not initialized, or is in its default disabled state (no session)
          // so let's fire-n-forget the init, without starting playback
          playbackController.startSession(
            itemId = currentSession.libraryItem.id,
            playImmediately = false,
          )
        } else {
          dbark { "<!-- Player already initialized [${playerSessionId}, $playerState]" }
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observePlayerState(
    session: Session?,
  ): PlayerUiState {
    val scope = rememberCoroutineScope()

    val player by remember {
      audioPlayerHolder.currentPlayer
    }.collectAsState()

    val time by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.currentTime.map { time ->
            time.inWholeSeconds.seconds
          }
        }
    }.collectAsState(Duration.ZERO)

    val duration by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.currentDuration
        }
    }.collectAsState(Duration.ZERO)

    val metadata by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.currentMetadata
        }
    }.collectAsState(Metadata())

    val state by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.state
        }
    }.collectAsState(AudioPlayer.State.Disabled)

    val speed by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.playbackSpeed
        }
    }.collectAsState(1f)

    val timer by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.runningTimer
        }
    }.collectAsState(null)

    return PlayerUiState(
      time = time,
      duration = duration,
      metadata = metadata,
      state = state,
      speed = speed,
      timer = timer,
    ) { event ->
      when (event) {
        PlayerUiEvent.PlayPauseClick -> {
          if (state == AudioPlayer.State.Disabled && session != null) {
            scope.launch {
              playbackController.startSession(session.libraryItem.id)
            }
          } else {
            player?.playPause()
          }
        }

        PlayerUiEvent.NextClick -> player?.skipToNext()
        PlayerUiEvent.PreviousClick -> player?.skipToPrevious()
        PlayerUiEvent.FastForwardClick -> player?.seekForward()
        PlayerUiEvent.RewindClick -> player?.seekBackward()
        PlayerUiEvent.ClearTimer -> player?.clearTimer()
        is PlayerUiEvent.BookmarkSelected -> player?.seekTo(event.bookmark.time)
        is PlayerUiEvent.ChapterSelected -> player?.seekTo(event.chapter.id)
        is PlayerUiEvent.AudioTrackSelected -> player?.seekTo(event.audioTrack.index - 1)
        is PlayerUiEvent.Seek.Percent -> player?.seekTo(event.percent)
        is PlayerUiEvent.Seek.Position -> player?.seekTo(event.position)
        is PlayerUiEvent.TimerSelected -> player?.setTimer(event.timer)
      }
    }
  }

  @Composable
  private fun observeQueueState(): QueueUiState {
    val scope = rememberCoroutineScope()

    val queue by remember {
      sessionQueue.observeAll()
    }.collectAsState(emptyList())

    val localQueue = remember(queue) {
      queue.toMutableStateList()
    }

    return QueueUiState(
      queue = localQueue,
    ) { event ->
      when (event) {
        is QueueUiEvent.ReorderItem -> {
          val fromIndex = localQueue.indexOfFirst { it.id == event.fromItemId }
          val toIndex = localQueue.indexOfFirst { it.id == event.toItemId }
          localQueue.add(toIndex, localQueue.removeAt(fromIndex))
        }

        QueueUiEvent.ReorderStopped -> scope.launch {
          sessionQueue.reorder(localQueue)
        }

        QueueUiEvent.ClearQueue -> scope.launch {
          sessionQueue.clear()
        }

        is QueueUiEvent.QueueItemClick -> {
          playbackController.startSession(event.item.id)
          scope.launch {
            sessionQueue.remove(event.item)
          }
        }

        is QueueUiEvent.RemoveQueueItem -> scope.launch {
          sessionQueue.remove(event.item)
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observeSyncState(
    session: Session?,
    expanded: Boolean,
  ): SyncUiState {
    val scope = rememberCoroutineScope()

    val mediaProgress by remember(expanded) {
      snapshotFlow { session?.libraryItem?.id }
        .filterNotNull()
        .flatMapLatest { libraryItemId ->
          mediaProgressRepository.observeProgress(libraryItemId, refresh = true)
        }
    }.collectAsState(null)

    val availableSync by remember(session) {
      derivedStateOf {
        if (
          session != null && mediaProgress != null &&
          (session.lastPlayedAt?.epochMilliseconds ?: 0L) < mediaProgress!!.lastUpdate &&
          session.currentTime.inWholeSeconds != mediaProgress!!.currentTime.seconds.inWholeSeconds
        ) {
//          val syncTimeInMillis = mediaProgress!!.currentTime.seconds.inWholeMilliseconds
//          val targetContentTitle = session.libraryItem.getChapterForDuration(syncTimeInMillis)
//            ?.takeIf { it.id != session.chapter?.id }
//            ?.title
//            ?: session.libraryItem.getAudioTrackForDuration(syncTimeInMillis)
//              ?.takeIf { it.index != session.audioTrack?.index }
//              ?.taggedTitle

          AvailableSync(
            itemId = session.libraryItem.id,
            currentTime = session.currentTime,
            targetTime = mediaProgress!!.currentTime.seconds,
            syncTimeInMillis = mediaProgress!!.lastUpdate,
            targetChapterTitle = null,
          )
        } else {
          null
        }
      }
    }

    return SyncUiState(
      mediaProgress = mediaProgress,
      availableSync = availableSync,
    ) { event ->
      when (event) {
        is SyncUiEvent.Sync -> {
          scope.launch {
            sessionsRepository.updateLastPlayed(event.libraryItemId)
          }
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observeThemeState(
    session: Session?,
  ): ThemeUiState {
    val isDynamicThemingEnabled by remember {
      themeSettings.observeDynamicallyThemePlayback()
    }.collectAsState()

    val theme by remember(session?.libraryItem?.id, isDynamicThemingEnabled) {
      if (!isDynamicThemingEnabled) {
        flowOf(null)
      } else {
        val itemId = session?.libraryItem?.id
        if (itemId != null) {
          themeManager.observeThemeFor(itemId)
        } else {
          emptyFlow()
        }
      }
    }.collectAsState(null)

    return ThemeUiState(
      dynamicThemingEnabled = isDynamicThemingEnabled,
      theme = theme,
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observeItemValidation(
    session: Session?,
  ): LibraryItemValidation {
    val itemValidation by remember {
      snapshotFlow { session?.libraryItem }
        .filterNotNull()
        .mapLatest { item ->
          libraryItemValidator.validate(item)
        }
    }.collectAsState(LibraryItemValidation.Success)

    return itemValidation
  }
}
