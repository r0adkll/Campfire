// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.core.logging.Cork
import java.util.Locale
import kotlin.math.floor
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.media.MediaRef
import uk.co.caprica.vlcj.player.base.Equalizer
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

/**
 * [PlaybackEngine] over libvlc via vlcj.
 *
 * libvlc delivers events on its own native thread and vlcj forbids calling back into libvlc from
 * that thread, so the listener below only translates events into [PlaybackEngineEvent]s; it never
 * touches the player. All control calls arrive on the owner's engine thread. Items are started with
 * the non-blocking `play` (not vlcj's latch-based `start`) and readiness is observed through events.
 */
class VlcPlaybackEngine private constructor(
  private val factory: MediaPlayerFactory,
  private val component: AudioPlayerComponent,
) : PlaybackEngine {

  private val player: MediaPlayer = component.mediaPlayer()
  private val equalizer: Equalizer = factory.equalizer().newEqualizer()

  private val _events = MutableSharedFlow<PlaybackEngineEvent>(
    extraBufferCapacity = EVENT_BUFFER,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )
  override val events: Flow<PlaybackEngineEvent> = _events.asSharedFlow()

  private var rate = 1f
  private var equalizerEnabled = false

  @Volatile
  private var cachedVolume = 1f

  override var volume: Float
    get() = cachedVolume
    set(value) {
      cachedVolume = value.coerceIn(0f, 1f)
      // floor() rather than rounding: the fade controller steps in small float increments and
      // rounding would snap the first steps straight back to 100.
      player.audio().setVolume(floor(cachedVolume * 100f).toInt())
    }

  init {
    player.events().addMediaPlayerEventListener(EventTranslator())
  }

  override fun open(item: MediaItem, startPosition: Duration, playWhenReady: Boolean) {
    val options = buildList {
      if (!playWhenReady) add(OPTION_START_PAUSED)
      if (startPosition > Duration.ZERO) {
        // libvlc's start-time is a float in seconds; format with millisecond precision so a
        // resume lands where the session left off rather than on the previous whole second.
        add(OPTION_START_TIME + String.format(Locale.ROOT, "%.3f", startPosition.inWholeMilliseconds / 1000.0))
      }
    }
    dbark { "open(${item.id}, start=$startPosition, playWhenReady=$playWhenReady) options=$options" }

    // Setting new media stops whatever is playing; play() returns without waiting for the
    // stream to open, and readiness arrives as opening/playing/error events.
    val accepted = player.media().play(item.uri, *options.toTypedArray())
    if (!accepted) {
      _events.tryEmit(PlaybackEngineEvent.Error(PlaybackEngineException("libvlc refused to open ${item.id}")))
      return
    }
    // Rate and equalizer are player-level settings in libvlc 3 and carry across media, but
    // re-applying is cheap and removes any doubt after a media change.
    player.controls().setRate(rate)
    applyEqualizer()
  }

  override fun play() {
    player.controls().play()
  }

  override fun pause() {
    player.controls().setPause(true)
  }

  override fun stop() {
    player.controls().stop()
  }

  override fun seekTo(position: Duration) {
    player.controls().setTime(position.inWholeMilliseconds)
  }

  override fun setRate(rate: Float) {
    this.rate = rate
    player.controls().setRate(rate)
  }

  override fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) {
    equalizerEnabled = enabled
    if (enabled) {
      equalizer.setPreamp(preampDb.coerceIn(VLC_GAIN_RANGE_DB))
      bandGainsDb.take(equalizer.bandCount()).forEachIndexed { index, gainDb ->
        equalizer.setAmp(index, gainDb.coerceIn(VLC_GAIN_RANGE_DB))
      }
    }
    applyEqualizer()
  }

  private fun applyEqualizer() {
    // (Re)setting the equalizer after amp changes is what makes libvlc pick them up
    player.audio().setEqualizer(if (equalizerEnabled) equalizer else null)
  }

  override fun release() {
    component.release()
    factory.release()
  }

  private inner class EventTranslator : MediaPlayerEventAdapter() {
    private fun emit(event: PlaybackEngineEvent) {
      _events.tryEmit(event)
    }

    override fun opening(mediaPlayer: MediaPlayer?) = emit(PlaybackEngineEvent.StateChanged(EngineState.Opening))

    override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
      // libvlc reports cache fill continuously while playing; only a partial cache is a stall
      if (newCache < FULL_CACHE) emit(PlaybackEngineEvent.StateChanged(EngineState.Buffering))
    }

    override fun playing(mediaPlayer: MediaPlayer?) = emit(PlaybackEngineEvent.StateChanged(EngineState.Playing))

    override fun paused(mediaPlayer: MediaPlayer?) = emit(PlaybackEngineEvent.StateChanged(EngineState.Paused))

    override fun stopped(mediaPlayer: MediaPlayer?) = emit(PlaybackEngineEvent.StateChanged(EngineState.Idle))

    override fun finished(mediaPlayer: MediaPlayer?) = emit(PlaybackEngineEvent.StateChanged(EngineState.Ended))

    override fun error(mediaPlayer: MediaPlayer?) {
      emit(PlaybackEngineEvent.Error(PlaybackEngineException("libvlc reported a playback error")))
    }

    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
      emit(PlaybackEngineEvent.PositionChanged(newTime.coerceAtLeast(0L).milliseconds))
    }

    override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
      if (newLength > 0L) emit(PlaybackEngineEvent.DurationChanged(newLength.milliseconds))
    }

    override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) {
      dbark { "mediaChanged($media)" }
    }
  }

  /**
   * Discovers and loads libvlc. Everything native happens here, on the engine thread, so a
   * missing or broken VLC install surfaces as [PlaybackEngineUnavailableException] instead of an
   * exception on whatever thread first touched the player.
   */
  class Factory(
    /** Extra libvlc command-line options, appended to the audio-only defaults. */
    private val libvlcArgs: List<String> = emptyList(),
  ) : PlaybackEngine.Factory {
    override fun create(): PlaybackEngine {
      // MediaPlayerFactory runs NativeDiscovery and loads libvlc. Failures surface as
      // RuntimeException (library not found) or UnsatisfiedLinkError (found but unloadable).
      val factory = try {
        MediaPlayerFactory(*(DEFAULT_LIBVLC_ARGS + libvlcArgs).toTypedArray())
      } catch (e: Exception) {
        throw PlaybackEngineUnavailableException("Unable to load VLC native libraries", e)
      } catch (e: LinkageError) {
        throw PlaybackEngineUnavailableException("Unable to link VLC native libraries", e)
      }
      return VlcPlaybackEngine(factory, AudioPlayerComponent(factory))
    }
  }

  companion object : Cork {
    override val tag: String = "VlcPlaybackEngine"
    override val enabled: Boolean = true

    private const val EVENT_BUFFER = 256

    // Same defaults vlcj applies for a headless audio player
    private val DEFAULT_LIBVLC_ARGS = listOf("--quiet", "--intf=dummy")
    private const val FULL_CACHE = 100f
    private const val OPTION_START_PAUSED = ":start-paused"
    private const val OPTION_START_TIME = ":start-time="

    // libvlc constrains equalizer preamp/amps to +/-20 dB (LibVlcConst.MIN_GAIN/MAX_GAIN)
    private val VLC_GAIN_RANGE_DB = -20f..20f
  }
}
