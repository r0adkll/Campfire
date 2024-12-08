package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
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
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.model.Session
import app.campfire.core.time.FatherTime
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@OptIn(UnstableApi::class)
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
  private val fatherTime: FatherTime,
  private val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context),
) : AudioPlayer, Player.Listener {

  @Inject
  class Factory(
    private val settings: PlaybackSettings,
    private val mediaSourceFactory: MediaSource.Factory,
    private val fatherTime: FatherTime,
  ) {

    fun create(context: Context): ExoPlayerAudioPlayer {
      return ExoPlayerAudioPlayer(
        context = context,
        settings = settings,
        mediaSourceFactory = mediaSourceFactory,
        fatherTime = fatherTime,
      )
    }
  }

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  internal val exoPlayer = ExoPlayer.Builder(context)
    .setSeekForwardIncrementMs(settings.forwardTimeMs)
    .setSeekBackIncrementMs(settings.backwardTimeMs)
    .setHandleAudioBecomingNoisy(true)
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
    .apply {
      addListener(this@ExoPlayerAudioPlayer)
    }

  private var progressJob: Job? = null

  private var playbackTimer: PlaybackTimer? = null
  private var playbackTimerJob: Job? = null

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(0.seconds)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)
  override val runningTimer = MutableStateFlow<RunningTimer?>(null)

  fun release() {
    scope.cancel()
  }

  override fun prepare(session: Session) {
    scope.launch {
      val mediaItems = MediaItemBuilder.build(session)

      exoPlayer.run {
        setMediaItems(mediaItems, true)

        // TODO: Prepare the session according to the current session
//        session.chapterProgress
//        seekTo(session.currentTime.inWholeMilliseconds)

        playWhenReady = true
        prepare()
      }
    }
  }

  override fun pause() {
    exoPlayer.pause()
  }

  override fun playPause() {
    if (exoPlayer.isPlaying) {
      exoPlayer.pause()
    } else {
      exoPlayer.play()
    }
  }

  override fun stop() {
    exoPlayer.stop()
  }

  override fun seekTo(itemIndex: Int) {
    exoPlayer.seekToDefaultPosition(itemIndex)
  }

  override fun seekTo(progress: Float) {
    val positionMs = (progress * exoPlayer.duration).toLong()
    exoPlayer.seekTo(positionMs)
    currentTime.value = positionMs.milliseconds
  }

  override fun skipToNext() {
    exoPlayer.seekToNextMediaItem()
  }

  override fun skipToPrevious() {
    exoPlayer.seekToPreviousMediaItem()
  }

  override fun seekForward() {
    exoPlayer.seekForward()
  }

  override fun seekBackward() {
    exoPlayer.seekBack()
  }

  override fun setPlaybackSpeed(speed: Float) {
    playbackSpeed.value = speed
    exoPlayer.setPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    clearTimer()
    playbackTimer = timer
    runningTimer.value = RunningTimer(timer, fatherTime.nowInEpochMillis())
    startTimer(timer)
  }

  private fun startTimer(timer: PlaybackTimer) {
    // TODO: abstract this logic?
    if (timer is PlaybackTimer.Epoch) {
      playbackTimerJob = scope.async {
        delay(timer.epochMillis)
        val sleepStartAtMs = fatherTime.nowInEpochMillis()
        val startVolume = exoPlayer.volume
        while (isActive && exoPlayer.volume > 0) {
          val elapsed = fatherTime.nowInEpochMillis() - sleepStartAtMs
          val progress = 1f - (elapsed.toFloat() / WhisperTime.toFloat()).coerceIn(0f..1f)

          exoPlayer.volume = (startVolume * progress).coerceAtLeast(0f)
          delay(150L)
        }
        exoPlayer.pause()
        clearTimer()
      }
    }
  }

  override fun clearTimer() {
    playbackTimerJob?.cancel()
    playbackTimerJob = null
    playbackTimer = null
    runningTimer.value = null
  }


  /*
   * Player Listener Callbacks
   */

  override fun onTimelineChanged(timeline: Timeline, reason: Int) {
    currentDuration.value = exoPlayer.duration.milliseconds
  }

  override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
    currentDuration.value = exoPlayer.duration.milliseconds
    currentMetadata.value = Metadata(
      title = mediaMetadata.title?.toString(),
    )
  }

  override fun onEvents(player: Player, events: Player.Events) {
    if (events.containsAny(
        EVENT_PLAYBACK_STATE_CHANGED,
        EVENT_PLAY_WHEN_READY_CHANGED,
        EVENT_IS_PLAYING_CHANGED,
    )) {
      state.value = when (player.playbackState) {
        Player.STATE_BUFFERING -> AudioPlayer.State.Buffering
        Player.STATE_READY -> when (player.isPlaying) {
          true -> AudioPlayer.State.Playing
          false -> AudioPlayer.State.Paused
        }

        else -> AudioPlayer.State.Disabled
      }

      if (player.isPlaying) {
        observeProgress(player)
      } else {
        progressJob?.cancel()
      }
    }

    // If the media item transitions (i.e. chapter) and the timer is
    // end of chapter, then stop the playback
    if (events.containsAny(EVENT_MEDIA_ITEM_TRANSITION)) {
      if (playbackTimer is PlaybackTimer.EndOfChapter) {
        player.pause()
        clearTimer()
      }
    }
  }

  private fun observeProgress(player: Player) {
    progressJob?.cancel()
    progressJob = scope.launch {
      while (isActive) {
        updateProgress(player)
        delay(500L)
      }
    }
  }

  private fun updateProgress(player: Player) {
    currentTime.value = player.currentPosition.milliseconds
    currentDuration.value = player.duration.milliseconds

    var timelineOffsetMs = 0L
    val timeline = player.currentTimeline
    if (!timeline.isEmpty) {
      val currentIndex = player.currentMediaItemIndex
      if (currentIndex > 0 && currentIndex < timeline.windowCount) {
        (0 until currentIndex).forEach { index ->
          val window = Timeline.Window()
          timeline.getWindow(index, window)
          timelineOffsetMs += window.durationMs
        }
      }
    }

    overallTime.value = (timelineOffsetMs + player.currentPosition).milliseconds
  }
}

private const val WhisperTime = 5//s
