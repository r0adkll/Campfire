package app.campfire.audioplayer.impl

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.impl.mediaitem.ArtworkLoader
import app.campfire.audioplayer.impl.mediaitem.IosMediaItemBuilder
import app.campfire.audioplayer.impl.player.IosPlayer
import app.campfire.audioplayer.impl.player.NowPlaying
import app.campfire.audioplayer.impl.player.enable
import app.campfire.audioplayer.impl.player.getPreferredIntervals
import app.campfire.audioplayer.impl.player.preferredIntervals
import app.campfire.audioplayer.impl.player.supportedPlaybackRates
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.audioplayer.impl.sleep.VolumeFadeController
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPChangePlaybackRateCommandEvent
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandEvent
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandHandlerStatusCommandFailed
import platform.MediaPlayer.MPRemoteCommandHandlerStatusNoSuchContent
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPSkipIntervalCommand
import platform.UIKit.UIApplication
import platform.UIKit.beginReceivingRemoteControlEvents
import platform.UIKit.endReceivingRemoteControlEvents

class IosAudioPlayer(
  private val settings: PlaybackSettings,
  private val fatherTime: FatherTime,
  private val artworkLoader: ArtworkLoader,
  sleepTimerManagerFactory: SleepTimerManager.Factory,
) : AudioPlayer {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val sleepTimerManager = sleepTimerManagerFactory.create(this)

  override var preparedSession: Session? = null
  private var finishedListener: OnFinishedListener? = null

  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(settings.playbackSpeed)
  override val runningTimer: StateFlow<RunningTimer?>
    get() = sleepTimerManager.runningTimer

  private val player = IosPlayer(
    scope = scope,
    skipToPreviousResetThreshold = settings.trackResetThreshold,
    onFinished = {
      scope.launch {
        finishedListener?.invoke(
          preparedSession?.libraryItem?.id ?: return@launch,
        )
      }
    },
  )

  override val state: StateFlow<AudioPlayer.State> = player.state
  override val currentTime: StateFlow<Duration> = player.currentPosition
  override val overallTime: StateFlow<Duration> = player.overallPosition
  override val currentDuration: StateFlow<Duration> = player.currentDuration

  private var fadeJob: Job? = null
  private var previousVolumeLevel: Float = 0f

  init {
    bark { "Initializing $this" }

    player.currentMetadata
      .onEach { mediaMetadata ->
        currentMetadata.value = Metadata(
          title = mediaMetadata?.title,
          artworkUri = mediaMetadata?.artworkUri,
        )

        sleepTimerManager.endOfChapter()
      }
      .launchIn(scope)
  }

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) {
    preparedSession = session
    finishedListener = onFinished
    player.prePrepare()

    setupRemoteTransportControls()

    // Setup now playing with session and artwork information
    scope.launch {
      NowPlaying.updateSession(session)
      artworkLoader.load(session.libraryItem.media.coverImageUrl)?.let { artwork ->
        NowPlaying.updateSession(session, artwork)
      }
    }

    // Build the media items and adapt them to the platform
    val mediaItems = IosMediaItemBuilder.build(session)
    player.setMediaItems(mediaItems)

    player.setPlaybackSpeed(playbackSpeed.value)

    // Seek the media player
    var startTimeInChapterMs = 0L
    if (chapterId != null) {
      val chapter = session.libraryItem.media.chapters.find { it.id == chapterId }
        ?: error("Unable to find chapter to start")

      for (index in mediaItems.indices) {
        val item = mediaItems[index]
        val track = item.tracks.find { it.id == chapterId }
        if (track != null) {
          startTimeInChapterMs = track.startMs - item.startOffset.inWholeMilliseconds
          player.currentItemIndex = index
          break
        }
      }

      bark {
        """
          Preparing iOS media player(
            chapter = $chapter,
            overallProgress = $startTimeInChapterMs,
            chapterId = $chapterId,
          )
        """.trimIndent()
      }
    } else if (session.currentTime.isFinite() && session.currentTime > 0.seconds) {
      val chapter = session.chapter
      val progressInChapter = (session.currentTime - chapter.start.seconds)

      val timestampInMillis = session.currentTime.inWholeMilliseconds
      var mediaItemOffsetMs = 0L
      for (index in mediaItems.indices) {
        val mediaItem = mediaItems[index]
        val mediaItemDuration = mediaItem.duration.inWholeMilliseconds
        val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
        if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
          player.currentItemIndex = index
          startTimeInChapterMs = timestampInMillis - mediaItemOffsetMs
          break
        }
        mediaItemOffsetMs = mediaItemEnd
      }

      bark {
        """
          Preparing iOS media player(
            chapter = $chapter,
            progressInChapter = $progressInChapter,
            mediaItemIndex = ${player.currentItemIndex},
            startTimeInChapterMs = $startTimeInChapterMs,
            session-currentTime = ${session.currentTime.inWholeMilliseconds}
          )
        """.trimIndent()
      }

      // Hydrate the current states so the UI reflects appropriately
      currentMetadata.value = Metadata(
        title = chapter.title,
        artworkUri = session.libraryItem.media.coverImageUrl,
      )
    }

    if (playImmediately) {
      sleepTimerManager.onSessionStart()
    }

    player.prepare(playImmediately, startTimeInChapterMs)
  }

  override fun release() {
    stop()
  }

  override fun pause() {
    player.pause()
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    previousVolumeLevel = player.volume
    fadeJob?.cancel()

    return VolumeFadeController.fade(
      scope = scope,
      duration = duration,
      tickRate = tickRate,
      getVolume = { player.volume },
      setVolume = { player.volume = it },
      onPause = { player.pause() },
    ).also { fadeJob = it }
  }

  override fun playPause() {
    if (state.value == AudioPlayer.State.Paused) {
      sleepTimerManager.onSessionStart()
    }

    // Reset volume if stored
    if (player.volume == 0f && previousVolumeLevel > 0f) {
      player.volume = previousVolumeLevel
    } else if (player.volume == 0f) {
      player.volume = 1f
    }

    player.playPause()
  }

  override fun stop() {
    preparedSession = null
    finishedListener = null
    player.close()
    scope.launch {
      UIApplication.sharedApplication.endReceivingRemoteControlEvents()
    }.invokeOnCompletion {
      scope.cancel()
    }
  }

  override fun seekTo(itemIndex: Int) {
    // From the UI perspective [itemIndex] is the chapter id when making this call
    // However, since we don't have a 1-to-1 mapping of chapters -> MediaItems in the
    // iOS player [itemIndex] is essentially track id and the player will find and jump
    // accordingly
    player.seekTo(itemIndex)
  }

  override fun seekTo(progress: Float) {
    player.seekTo(progress)
  }

  override fun seekTo(timestamp: Duration) {
    player.seekTo(timestamp)
  }

  override fun skipToNext() {
    player.skipToNext()
  }

  override fun skipToPrevious() {
    player.skipToPrevious()
  }

  override fun seekForward() {
    player.seekForward(settings.forwardTimeMs)
  }

  override fun seekBackward() {
    player.seekBackward(settings.backwardTimeMs)
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    settings.playbackSpeed = speed
    player.setPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    sleepTimerManager.setTimer(timer)
  }

  override fun clearTimer() {
    sleepTimerManager.clearTimer()
  }

  private fun setupRemoteTransportControls() {
    scope.launch {
      UIApplication.sharedApplication.beginReceivingRemoteControlEvents()
    }

    val commandCenter = MPRemoteCommandCenter.sharedCommandCenter()

    val playPauseHandler: (MPRemoteCommandEvent) -> MPRemoteCommandHandlerStatus = {
      playPause()
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.playCommand.enable(action = playPauseHandler)
    commandCenter.pauseCommand.enable(action = playPauseHandler)
    commandCenter.togglePlayPauseCommand.enable(action = playPauseHandler)

    commandCenter.skipForwardCommand.enable(
      setup = { preferredIntervals(settings.forwardTimeMs) },
      action = { event ->
        when (val command = event.command) {
          is MPSkipIntervalCommand -> {
            command.getPreferredIntervals().firstOrNull()?.let { interval ->
              player.seekForward(interval.inWholeMilliseconds)
              MPRemoteCommandHandlerStatusSuccess
            } ?: MPRemoteCommandHandlerStatusCommandFailed
          }
          else -> MPRemoteCommandHandlerStatusNoSuchContent
        }
      },
    )

    commandCenter.skipBackwardCommand.enable(
      setup = { preferredIntervals(settings.backwardTimeMs) },
      action = { event ->
        when (val command = event.command) {
          is MPSkipIntervalCommand -> {
            command.getPreferredIntervals().firstOrNull()?.let { interval ->
              player.seekBackward(interval.inWholeMilliseconds)
              MPRemoteCommandHandlerStatusSuccess
            } ?: MPRemoteCommandHandlerStatusCommandFailed
          }
          else -> MPRemoteCommandHandlerStatusNoSuchContent
        }
      },
    )

    commandCenter.nextTrackCommand.enable {
      skipToNext()
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.previousTrackCommand.enable {
      skipToPrevious()
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.changePlaybackPositionCommand.enable {
      val event = it as? MPChangePlaybackPositionCommandEvent
        ?: return@enable MPRemoteCommandHandlerStatusNoSuchContent
      val newProgress = event.positionTime.seconds / currentDuration.value
      player.seekTo(newProgress.toFloat())
      MPRemoteCommandHandlerStatusSuccess
    }

    commandCenter.changePlaybackRateCommand.enable(
      setup = { supportedPlaybackRates(settings.playbackRates) },
      action = { event ->
        val playbackRateEvent = event as? MPChangePlaybackRateCommandEvent
          ?: return@enable MPRemoteCommandHandlerStatusNoSuchContent
        setPlaybackSpeed(playbackRateEvent.playbackRate)
        MPRemoteCommandHandlerStatusSuccess
      },
    )
  }
}
