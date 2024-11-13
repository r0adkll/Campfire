package app.campfire.audioplayer

import kotlin.time.Duration
import kotlinx.coroutines.flow.StateFlow

/**
 * The interface by which to interact with the actual media controls
 */
interface AudioPlayer {

  val state: StateFlow<State>
  val currentTime: StateFlow<Duration>
  val currentDuration: StateFlow<Duration>
  val playbackSpeed: StateFlow<Float>

  fun pause()
  fun playPause()
  fun stop()
  fun seekTo(positionInMs: Long)
  fun seekTo(progress: Float)

  fun skipToNext()
  fun skipToPrevious()

  fun seekForward()
  fun seekBackward()

  fun setPlaybackSpeed(speed: Float)

  enum class State {
    Disabled,
    Buffering,
    Playing,
    Paused,
  }
}
