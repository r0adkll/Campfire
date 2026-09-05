// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.impl.mediaitem.MediaItem
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakePlaybackEngine : PlaybackEngine {

  data class Open(
    val item: MediaItem,
    val startPosition: Duration,
    val playWhenReady: Boolean,
    val headers: Map<String, String> = emptyMap(),
  )

  data class EqualizerCall(val enabled: Boolean, val preampDb: Float, val bandGainsDb: List<Float>)

  val opens = mutableListOf<Open>()
  val seeks = mutableListOf<Duration>()
  var plays = 0
  var pauses = 0
  var stops = 0
  var released = false
  var currentRate = 1f
  var equalizer: EqualizerCall? = null

  private val _events = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = 64)
  override val events: Flow<PlaybackEngineEvent> = _events

  override var volume: Float = 1f

  override var supportsRequestHeaders: Boolean = false

  /** Deliver an event as the native library would; the player must be collecting. */
  fun emit(event: PlaybackEngineEvent) {
    check(_events.tryEmit(event)) { "Event dropped: $event" }
  }

  override fun open(item: MediaItem, startPosition: Duration, playWhenReady: Boolean, headers: Map<String, String>) {
    opens += Open(item, startPosition, playWhenReady, headers)
  }

  override fun play() {
    plays++
  }

  override fun pause() {
    pauses++
  }

  override fun stop() {
    stops++
  }

  override fun seekTo(position: Duration) {
    seeks += position
  }

  override fun setRate(rate: Float) {
    currentRate = rate
  }

  override fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) {
    equalizer = EqualizerCall(enabled, preampDb, bandGainsDb)
  }

  override fun release() {
    released = true
  }
}
