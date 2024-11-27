package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Player.EVENT_IS_PLAYING_CHANGED
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
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.logging.bark
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
import me.tatarka.inject.annotations.Inject

@OptIn(UnstableApi::class)
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
  private val mediaSourceFactory: MediaSource.Factory = DefaultMediaSourceFactory(context),
) : AudioPlayer, Player.Listener {

  @Inject
  class Factory(
    private val settings: PlaybackSettings,
    private val mediaSourceFactory: MediaSource.Factory,
  ) {

    fun create(context: Context): ExoPlayerAudioPlayer {
      return ExoPlayerAudioPlayer(
        context = context,
        settings = settings,
        mediaSourceFactory = mediaSourceFactory,
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

  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val currentTime = MutableStateFlow(0.seconds)
  override val currentDuration = MutableStateFlow(0.seconds)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)

  fun release() {
    scope.cancel()
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
  }

  private val Timeline.Window.windowEndTimeMs: Long get() = windowStartTimeMs + durationMs
}
