package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.DeviceInfo
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_DEVICE_INFO_CHANGED
import androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED
import androidx.media3.common.Player.EVENT_MEDIA_ITEM_TRANSITION
import androidx.media3.common.Player.EVENT_PLAYBACK_STATE_CHANGED
import androidx.media3.common.Player.EVENT_PLAY_WHEN_READY_CHANGED
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.impl.cast.SafeCastContext
import app.campfire.audioplayer.impl.mediaitem.MediaItemBuilder
import app.campfire.audioplayer.impl.sleep.SleepTimerManager
import app.campfire.audioplayer.impl.sleep.VolumeFadeController
import app.campfire.audioplayer.impl.util.AUDIO_TAG
import app.campfire.audioplayer.impl.util.eventAsDebugLog
import app.campfire.audioplayer.impl.util.playbackStateAsDebugLog
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Cork
import app.campfire.core.logging.Corked
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@OptIn(UnstableApi::class)
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
  private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  private val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context),
) : AudioPlayer, Player.Listener, Cork {

  override val tag: String = AUDIO_TAG

  // Re-enable to emit verbose logging around Player.Listener events for
  // debugging.
  private val eventLogger = Corked("Player.Listener.Event", enabled = false)

  @Inject
  class Factory(
    private val settings: PlaybackSettings,
    private val mediaSourceFactory: MediaSource.Factory,
    private val sleepTimerManagerFactory: SleepTimerManager.Factory,
  ) {

    fun create(context: Context): ExoPlayerAudioPlayer {
      return ExoPlayerAudioPlayer(
        context = context,
        settings = settings,
        mediaSourceFactory = mediaSourceFactory,
        sleepTimerManagerFactory = sleepTimerManagerFactory,
      )
    }
  }

  private val sleepTimerManager = sleepTimerManagerFactory.create(this)

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val exoPlayer = ExoPlayer.Builder(context)
    .setSeekForwardIncrementMs(settings.forwardTimeMs)
    .setSeekBackIncrementMs(settings.backwardTimeMs)
    .setHandleAudioBecomingNoisy(true)
    .setAudioAttributes(
      AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .setUsage(C.USAGE_MEDIA)
        .build(),
      true,
    )
    .setLoadControl(
      DefaultLoadControl.Builder()
        .setBufferDurationsMs(
          20 * 1000,
          45 * 1000,
          5 * 1000,
          20 * 1000,
        )
        .build(),
    )
    .setBandwidthMeter(
      DefaultBandwidthMeter.Builder(context)
        .build(),
    )
    .setMediaSourceFactory(mediaSourceFactory)
    .build()

  private val castPlayer: CastPlayer? = SafeCastContext.getContext(context)?.let {
    CastPlayer.Builder(context)
      .setLocalPlayer(exoPlayer)
      .build()
  }

  /**
   * A proxy accessor for the correct player client. All listener and access goes through the
   * [castPlayer] which configures the [exoPlayer] as the local player to use when a MediaRoute
   * is not directed at remote playback.
   */
  internal val player: Player = (castPlayer ?: exoPlayer).apply {
    addListener(this@ExoPlayerAudioPlayer)
  }

  private var progressJob: Job? = null
  private var fadeJob: Job? = null
  private var previousVolumeLevel: Float = 0f

  override var preparedSession: Session? = null
  private var finishedListener: OnFinishedListener? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(settings.playbackSpeed)

  override val runningTimer: StateFlow<RunningTimer?>
    get() = sleepTimerManager.runningTimer

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) = withContext(Dispatchers.Main) {
    preparedSession = session
    finishedListener = onFinished
    state.value = AudioPlayer.State.Initializing

    val mediaItems = MediaItemBuilder.build(session).asPlatformMediaItems()

    ibark {
      """
        Prepare Session(
          itemId = ${session.libraryItem.id.loggableId},
          playMethod = ${session.playMethod},
          mediaPlayer = ${session.mediaPlayer},
          currentTime = ${session.currentTime},
          chapterId = $chapterId,
        )
      """.trimIndent()
    }

    player.run {
      // Set the media list
      setMediaItems(mediaItems, true)

      // Set the playback speed
      setPlaybackSpeed(playbackSpeed.value)

      // Seek the media player
      if (chapterId != null) {
        // If the Chapter Id is passed explicitly then we can take that intention as
        // starting playback directly at that chapter
        val chapter = session.libraryItem.media.chapters.find { it.id == chapterId }
          ?: error("Unable to find chapter to start")

        val overallProgressOfChapterMs = session.libraryItem.media.chapters.fold(0L) { acc, c ->
          if (c.id < chapterId) {
            acc + c.duration.inWholeMilliseconds
          } else {
            acc
          }
        }

        seekTo(chapterId)
        currentTime.value = 0.seconds
        currentDuration.value = chapter.duration
        currentMetadata.value = Metadata(
          title = chapter.title,
          artworkUri = session.libraryItem.media.coverImageUrl,
        )
        overallTime.value = overallProgressOfChapterMs.milliseconds
      } else if (session.currentTime.isFinite() && session.currentTime > 0.seconds) {
        val chapter = session.chapter
        val progressInChapterMs = (session.currentTime - chapter.start.seconds)
          .inWholeMilliseconds.coerceAtLeast(0L)
        seekTo(chapter.id, progressInChapterMs)

        // Hydrate the current states so the UI reflects appropriately
        currentTime.value = progressInChapterMs.milliseconds
        currentDuration.value = chapter.duration
        currentMetadata.value = Metadata(
          title = chapter.title,
          artworkUri = session.libraryItem.media.coverImageUrl,
        )
        overallTime.value = session.currentTime
      }

      if (playImmediately) {
        // Potentially trigger the auto sleep timer
        sleepTimerManager.onSessionStart()
      }

      // Set when to play, and prepare
      playWhenReady = playImmediately
      prepare()
    }
  }

  override fun release() {
    preparedSession = null
    finishedListener = null
    scope.cancel()
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
    if (player.isPlaying) {
      player.pause()
    } else {
      // Potentially trigger the auto sleep timer
      sleepTimerManager.onSessionStart()

      // Reset volume if stored
      if (player.volume == 0f && previousVolumeLevel > 0f) {
        player.volume = previousVolumeLevel
      } else if (player.volume == 0f) {
        player.volume = 1f
      }

      player.play()
    }
  }

  override fun stop() {
    preparedSession = null
    finishedListener = null
    player.stop()
  }

  override fun seekTo(itemIndex: Int) {
    player.seekToDefaultPosition(itemIndex)
    player.play()
  }

  override fun seekTo(progress: Float) {
    val positionMs = (progress * player.duration).toLong()
    player.seekTo(positionMs)
    currentTime.value = positionMs.milliseconds
  }

  override fun seekTo(timestamp: Duration) {
    val timestampInMillis = timestamp.inWholeMilliseconds
    var mediaItemOffsetMs = 0L

    for (index in 0 until player.mediaItemCount) {
      val mediaItem = player.getMediaItemAt(index)
      val mediaItemDuration = mediaItem.mediaMetadata.durationMs ?: error("Media Metadata Corrupted")
      val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
      if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
        val progressInMediaItem = timestampInMillis - mediaItemOffsetMs
        player.seekTo(index, progressInMediaItem)
        player.play()
        return
      }
      mediaItemOffsetMs = mediaItemEnd
    }
  }

  override fun skipToNext() {
    player.seekToNextMediaItem()
  }

  override fun skipToPrevious() {
    if (player.currentPosition.milliseconds > settings.trackResetThreshold) {
      player.seekToDefaultPosition()
      player.play()
    } else {
      player.seekToPreviousMediaItem()
    }
  }

  override fun seekForward() {
    player.seekForward()
  }

  override fun seekBackward() {
    player.seekBack()
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

  /*
   * Player Listener Callbacks
   */

  override fun onDeviceInfoChanged(deviceInfo: DeviceInfo) {
    if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_LOCAL) {
      dbark { "onDeviceInfoChanged: Local Player" }
    } else if (deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE) {
      dbark { "onDeviceInfoChanged: Remote Player" }
    }
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_ENDED) {
      dbark { "Playback has ended! Mark the item as finished" }
      scope.launch {
        finishedListener?.invoke(preparedSession?.libraryItem?.id ?: return@launch)
      }
    } else if (
      playbackState == Player.STATE_READY &&
      player.deviceInfo.playbackType == DeviceInfo.PLAYBACK_TYPE_REMOTE
    ) {
      // Ensure that our remote cast player has up-to-date playback speed set
      // See: https://github.com/androidx/media/issues/889
      player.setPlaybackSpeed(settings.playbackSpeed)
    }
  }

  override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    currentDuration.value = player.duration.milliseconds
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    currentDuration.value = player.duration.milliseconds
    currentMetadata.value = Metadata(
      title = mediaMetadata.title?.toString(),
      artworkUri = mediaMetadata.artworkUri?.toString(),
    )
  }

  override fun onEvents(player: Player, events: Player.Events) {
    eventLogger.vbark {
      buildString {
        appendLine("onEvents {")
        for (i in 0 until events.size()) {
          appendLine("  ${events.get(i).eventAsDebugLog()}")
        }
        appendLine("}")
      }
    }

    if (events.containsAny(
        EVENT_PLAYBACK_STATE_CHANGED,
        EVENT_PLAY_WHEN_READY_CHANGED,
        EVENT_IS_PLAYING_CHANGED,
      )
    ) {
      state.value = when (player.playbackState) {
        Player.STATE_BUFFERING -> AudioPlayer.State.Buffering
        Player.STATE_READY -> when (player.isPlaying) {
          true -> AudioPlayer.State.Playing
          false -> AudioPlayer.State.Paused
        }

        Player.STATE_ENDED -> AudioPlayer.State.Finished

        else -> if (events.contains(EVENT_DEVICE_INFO_CHANGED)) {
          // This is likely due to changing media routes, i.e. Google Cast, and we should
          // just update the state as buffering and assume that future events will update the
          // state appropriately
          AudioPlayer.State.Buffering
        } else {
          AudioPlayer.State.Disabled
        }
      }

      eventLogger.vbark { "PlaybackState[${player.playbackStateAsDebugLog}] Changed: ${state.value}" }

      if (player.isPlaying) {
        observeProgress(player)
      } else {
        progressJob?.cancel()
      }
    }

    // If the media item transitions (i.e. chapter) and the timer is
    // end of chapter, then stop the playback
    if (events.containsAny(EVENT_MEDIA_ITEM_TRANSITION)) {
      sleepTimerManager.endOfChapter()
    }
  }

  private fun observeProgress(player: Player) {
    progressJob?.cancel()
    progressJob = scope.launch {
      dbark { "Starting Progress Observer" }
      while (isActive) {
        updateProgress(player)
        delay(500L)
      }
    }
    progressJob?.invokeOnCompletion {
      dbark { "Finished Progress Observer" }
    }
  }

  private fun updateProgress(player: Player) {
    currentTime.value = player.currentPosition.milliseconds
    currentDuration.value = player.duration.milliseconds

    var timelineOffsetMs = 0L
    val currentIndex = player.currentMediaItemIndex
    (0 until currentIndex).forEach { index ->
      timelineOffsetMs += player.getMediaItemAt(index)
        .mediaMetadata
        .durationMs
        ?: 0L
    }

    overallTime.value = (timelineOffsetMs + player.currentPosition).milliseconds
  }
}
