package app.campfire.audioplayer

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.mp3.Mp3Extractor
import app.campfire.audioplayer.model.PlaybackSession
import app.campfire.audioplayer.model.toMediaItem
import app.campfire.common.settings.PlaybackSettings
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.PlayMethod.DirectPlay
import app.campfire.core.model.PlayMethod.Local

@UnstableApi
class ExoPlayerAudioPlayer(
  private val context: Context,
  private val settings: PlaybackSettings,
) : AudioPlayer {

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
        .build()
    )
    .setBandwidthMeter(
      DefaultBandwidthMeter.Builder(context)
        .build()
    )
    .build()

  override fun prepare(session: PlaybackSession, playWhenReady: Boolean, playbackRate: Float?) {
    // Validate session
    if (session.tracks.isEmpty()) {
      bark(LogPriority.ERROR) { "Invalid PlaybackSession" }
      return
    }

    // Configure Playlist
    val mediaItems = session.tracks.map { track ->
      track.toMediaItem()
    }

    // Configure Audio Source
    val mediaSource = when (session.playMethod) {
      DirectPlay -> {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
        val extractorsFactory = DefaultExtractorsFactory()

        if (settings.enableMp3IndexSeeking) {
          // https://exoplayer.dev/troubleshooting.html#why-is-seeking-inaccurate-in-some-mp3-files
          extractorsFactory.setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
        }

        ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
          .createMediaSource(mediaItems.first())
      }
      Local -> {
        val dataSourceFactory = DefaultDataSource.Factory(context)
        val extractorsFactory = DefaultExtractorsFactory()

        ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
          .createMediaSource(mediaItems.first())
      }
      // Assume all other methods are HLS
      else -> {
        val datasourceFactory = DefaultHttpDataSource.Factory()
          .setDefaultRequestProperties(mapOf("Authorization" to "Bearer ${session.token}"))

        HlsMediaSource.Factory(datasourceFactory)
          .createMediaSource(mediaItems.first())
      }
    }
    exoPlayer.setMediaSource(mediaSource)

    if (mediaItems.size > 1) {
      exoPlayer.addMediaItems(mediaItems.subList(1, mediaItems.size))

      // TODO: Seek the player to the appropriate position
    } else {
      // TODO: Seek the player to the appropriate position

    }

    // Prepare and Play
    exoPlayer.playWhenReady = playWhenReady
    exoPlayer.prepare()
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

  override fun seekTo(positionInMs: Long) {
    exoPlayer.seekTo(positionInMs)
  }

  override fun skipToNext() {
    exoPlayer.seekToNextMediaItem()
  }

  override fun skipToPrevious() {
    exoPlayer.seekToPreviousMediaItem()
  }

  override fun seekForward(amount: Long?) {
    exoPlayer.seekForward()
  }

  override fun seekBackward(amount: Long?) {
    exoPlayer.seekToPrevious()
  }

  override fun setPlaybackSpeed(speed: Float) {
    exoPlayer.setPlaybackSpeed(speed)
  }
}
