package app.campfire.sessions.ui.playback

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
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
import app.campfire.core.coroutines.flatMapIfNotNull
import app.campfire.core.extensions.asDateTime
import app.campfire.core.extensions.epochMilliseconds
import app.campfire.core.extensions.readableFormat
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Corked
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import app.campfire.libraries.api.LibraryItemValidation
import app.campfire.libraries.api.LibraryItemValidator
import app.campfire.sessions.api.SessionQueue
import app.campfire.sessions.api.SessionsRepository
import app.campfire.settings.api.PlaybackSettings
import app.campfire.settings.api.ThemeSettings
import app.campfire.ui.theming.api.ThemeManager
import app.campfire.user.api.MediaProgressRepository
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
  private val playbackSettings: PlaybackSettings,
  private val audioPlayerHolder: AudioPlayerHolder,
  private val themeSettings: ThemeSettings,
  private val themeManager: ThemeManager,
) : Corked("PlaybackPresenter") {

  @Composable
  fun present(expanded: Boolean): PlaybackUiState {
    val scope = rememberCoroutineScope()

    // Check if we should initializer the playback session
    CheckInitializePlayerSession()

    // Don't use the getValue delegate here as the sub-uiState functions rely on
    // snapshotFlow and other mechanisms that require reading this state's value directly.
    // Otherwise, these functions that rely on the Compose snapshot/read mechanisms won't
    // work when reading the session.
    val currentSession = remember {
      sessionsRepository.observeCurrentSession()
    }.collectAsState(null)

    val playerState = observePlayerState(currentSession)
    val queueState = observeQueueState()
    val syncState = observeSyncState(currentSession, expanded)
    val themeState = observeThemeState(currentSession)
    val itemValidation = observeItemValidation(currentSession)
    val playbackHistoryEnabled by remember { playbackSettings.observePlaybackHistoryEnabled() }.collectAsState()

    return PlaybackUiState(
      session = currentSession.value,
      playerState = playerState,
      queueState = queueState,
      themeState = themeState,
      syncUiState = syncState,
      validation = itemValidation,
      playbackHistoryEnabled = playbackHistoryEnabled,
    ) { event ->
      when (event) {
        PlaybackUiEvent.ClearSession -> {
          currentSession.value?.let { s ->
            scope.launch {
              playbackController.stopSession(
                itemId = s.libraryItem.id,
                clearQueue = true,
                episodeId = s.episodeId,
              )
            }
          }
        }

        PlaybackUiEvent.StartSession -> {
          currentSession.value?.let { s ->
            scope.launch {
              playbackController.startSession(
                itemId = s.libraryItem.id,
                episodeId = s.episodeId,
              )
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
          dbark { "<~~ Player not initialized [$playerSessionId, $playerState]" }

          // Since we want to auto-sync listening session progress let's make sure to refresh the current
          // sessions libraryItem media progress, if possible and enabled
          if (playbackSettings.syncEnabled && playbackSettings.autoSyncEnabled) {
            dbark { "~~> Refreshing media progress…" }
            val (progress, duration) = measureTimedValue {
              // Since this is a network operation we want to timebox refreshing the current progress
              // So we don't create an awkward delay when initialing the playback session for the UI
              withTimeoutOrNull(1.seconds) {
                mediaProgressRepository.getProgress(
                  libraryItemId = currentSession.libraryItem.id,
                  episodeId = currentSession.episodeId,
                  fresh = true,
                )
              }
            }
            dbark {
              "<~~ Refreshed media progress in $duration [${progress?.actualTime} @ " +
                "${progress?.lastUpdate?.asDateTime()?.readableFormat}]"
            }
          }

          ibark { "--> Starting playback controller initialization" }
          // Okay either the player is not initialized, or is in its default disabled state (no session)
          // so let's fire-n-forget the init, without starting playback
          playbackController.startSession(
            itemId = currentSession.libraryItem.id,
            playImmediately = false,
            episodeId = currentSession.episodeId,
          )
        } else {
          dbark { "<!-- Player already initialized [$playerSessionId, $playerState]" }
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observePlayerState(
    session: State<Session?>,
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

    val error by remember {
      snapshotFlow { player }
        .filterNotNull()
        .flatMapLatest {
          it.error
        }
    }.collectAsState(null)

    return PlayerUiState(
      time = time,
      duration = duration,
      metadata = metadata,
      state = state,
      speed = speed,
      timer = timer,
      error = error,
    ) { event ->
      when (event) {
        PlayerUiEvent.PlayPauseClick -> {
          val sessionValue = session.value
          if (state == AudioPlayer.State.Disabled && sessionValue != null) {
            scope.launch {
              playbackController.startSession(
                itemId = sessionValue.libraryItem.id,
                episodeId = sessionValue.episodeId,
              )
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
          val fromIndex = localQueue.indexOfFirst { it.key == event.fromKey }
          val toIndex = localQueue.indexOfFirst { it.key == event.toKey }
          localQueue.add(toIndex, localQueue.removeAt(fromIndex))
        }

        QueueUiEvent.ReorderStopped -> scope.launch {
          sessionQueue.reorder(localQueue)
        }

        QueueUiEvent.ClearQueue -> scope.launch {
          sessionQueue.clear()
        }

        is QueueUiEvent.QueueItemClick -> {
          playbackController.startSession(
            itemId = event.entry.libraryItemId,
            episodeId = event.entry.episodeId,
          )
          scope.launch {
            sessionQueue.remove(event.entry.libraryItemId, event.entry.episodeId)
          }
        }

        is QueueUiEvent.RemoveQueueItem -> scope.launch {
          sessionQueue.remove(event.entry.libraryItemId, event.entry.episodeId)
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observeSyncState(
    session: State<Session?>,
    expanded: Boolean,
  ): SyncUiState {
    val scope = rememberCoroutineScope()

    val syncEnabled by remember {
      playbackSettings.observeSyncEnabled()
    }.collectAsState()

    val mediaProgress by remember(expanded) {
      snapshotFlow {
        if (syncEnabled) {
          session.value?.let { it.libraryItem.id to it.episodeId }
        } else {
          null
        }
      }
        .filterNotNull()
        .flatMapLatest { (libraryItemId, episodeId) ->
          mediaProgressRepository.observeProgress(
            libraryItemId = libraryItemId,
            episodeId = episodeId,
            refresh = true,
          )
        }
    }.collectAsState(null)

    val availableSync by remember {
      derivedStateOf {
        val sessionValue = session.value
        if (
          syncEnabled &&
          sessionValue != null && mediaProgress != null &&
          mediaProgress?.source != MediaProgress.Source.Local &&
          (sessionValue.lastPlayedAt?.epochMilliseconds ?: 0L) < mediaProgress!!.lastUpdate &&
          sessionValue.currentTime.inWholeSeconds != mediaProgress!!.currentTime.seconds.inWholeSeconds
        ) {
          AvailableSync(
            itemId = sessionValue.libraryItem.id,
            currentTime = sessionValue.currentTime,
            targetTime = mediaProgress!!.currentTime.seconds,
            syncTimeInMillis = mediaProgress!!.lastUpdate,
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
        SyncUiEvent.Sync -> {
          if (availableSync == null) return@SyncUiState
          scope.launch {
            audioPlayerHolder.currentPlayer.value?.seekTo(availableSync!!.targetTime)
          }
        }
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Composable
  private fun observeThemeState(
    session: State<Session?>,
  ): ThemeUiState {
    val isDynamicThemingEnabled by remember {
      themeSettings.observeDynamicallyThemePlayback()
    }.collectAsState()

    val theme by remember {
      snapshotFlow {
        if (isDynamicThemingEnabled) {
          session.value?.libraryItem?.id
        } else {
          null
        }
      }
        .flatMapIfNotNull { libraryItemId ->
          themeManager.observeThemeFor(libraryItemId)
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
    session: State<Session?>,
  ): LibraryItemValidation {
    val itemValidation by remember {
      snapshotFlow { session.value?.libraryItem }
        .filterNotNull()
        .mapLatest { item ->
          libraryItemValidator.validate(item)
        }
    }.collectAsState(LibraryItemValidation.Success)

    return itemValidation
  }
}
