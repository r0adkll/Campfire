// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackEngineUnavailableException
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * A single-item audio engine: it plays exactly one [MediaItem] at a time and knows nothing about
 * queues, chapters, or sessions. [DesktopAudioPlayer][app.campfire.audioplayer.impl.DesktopAudioPlayer]
 * owns the queue and the chapter timeline and drives the engine one item at a time, so swapping
 * the underlying library (libvlc today, FFmpeg tomorrow) only means re-implementing this.
 *
 * Threading contract:
 * - Every method is invoked from the owner's single engine thread. Implementations may block that
 *   thread briefly (libvlc's `stop` is synchronous) but are never called from the UI thread.
 * - [events] may be emitted from any thread, including a native callback thread. Implementations
 *   must not call back into their own native library from the emitting thread; the owner collects
 *   the flow on the engine thread and reacts there.
 */
interface PlaybackEngine {

  /** Hot stream of engine events. Late subscribers see only new events. */
  val events: Flow<PlaybackEngineEvent>

  /**
   * The engine's output gain, 0f..1f. Reads return the last value written (no native call) so the
   * fade controller can poll it cheaply; the initial value is 1f.
   */
  var volume: Float

  /**
   * Replace whatever is loaded with [item], positioned at [startPosition] within the item. When
   * [playWhenReady] is false the item is opened but left paused at that position.
   */
  fun open(item: MediaItem, startPosition: Duration, playWhenReady: Boolean)

  fun play()

  fun pause()

  /** Stop and unload the current item. The engine emits [EngineState.Idle] once stopped. */
  fun stop()

  /** Seek within the currently open item. */
  fun seekTo(position: Duration)

  /** Playback rate with pitch preserved. Persists across [open] calls. */
  fun setRate(rate: Float)

  /**
   * Apply (or with [enabled] false, detach) the equalizer. [bandGainsDb] map index-for-index onto
   * the engine's bands; implementations clamp to their own limits. Persists across [open] calls.
   */
  fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>)

  /** Release native resources. The engine must not be used afterwards. */
  fun release()

  fun interface Factory {
    /**
     * Create an engine, loading native libraries as needed. Called on the engine thread.
     * @throws PlaybackEngineUnavailableException when the platform has no usable engine
     */
    fun create(): PlaybackEngine
  }
}

enum class EngineState {
  /** Nothing loaded, or the current item was stopped. */
  Idle,

  /** An item is being opened (connecting, probing). */
  Opening,

  /** Playback stalled waiting on data. */
  Buffering,
  Playing,
  Paused,

  /** The current item played to its end (or its configured stop point). */
  Ended,
}

sealed interface PlaybackEngineEvent {
  data class StateChanged(val state: EngineState) : PlaybackEngineEvent

  /** Position within the current item. */
  data class PositionChanged(val position: Duration) : PlaybackEngineEvent

  /** Duration of the current item as reported by the engine once it has probed the media. */
  data class DurationChanged(val duration: Duration) : PlaybackEngineEvent

  data class Error(val cause: Throwable) : PlaybackEngineEvent
}

/** A failure to open or play a specific item. */
class PlaybackEngineException(message: String, cause: Throwable? = null) : Exception(message, cause)
