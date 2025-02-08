package app.campfire.audioplayer

import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow

/**
 * The interface by which to interact with the actual media controls
 */
interface AudioPlayer {

  /**
   * The current session this player has been prepared with.
   */
  val preparedSession: Session?

  /**
   * A flow of the current playback state.
   */
  val state: StateFlow<State>

  /**
   * A flow of the overall playback time for the current audiobook playing
   */
  val overallTime: StateFlow<Duration>

  /**
   * A flow of the time in the current media item/track/chapter.
   */
  val currentTime: StateFlow<Duration>

  /**
   * A flow of the total duration of the current media time/track/chapter.
   */
  val currentDuration: StateFlow<Duration>

  /**
   * A flow of the metadata for the current media item/track/chapter.
   */
  val currentMetadata: StateFlow<Metadata>

  /**
   * A flow of the current playback speed
   */
  val playbackSpeed: StateFlow<Float>

  /**
   * A flow of the current playback timer
   */
  val runningTimer: StateFlow<RunningTimer?>

  suspend fun prepare(
    session: Session,
    playImmediately: Boolean = true,
    chapterId: Int? = null,
  )

  fun release()

  fun pause()
  fun fadeToPause(
    duration: Duration = DefaultFadeDuration,
    tickRate: Long = DefaultFadeTickRate,
  ): Job
  fun playPause()
  fun stop()
  fun seekTo(itemIndex: Int)
  fun seekTo(progress: Float)
  fun seekTo(timestamp: Duration)

  fun skipToNext()
  fun skipToPrevious()

  fun seekForward()
  fun seekBackward()

  fun setPlaybackSpeed(speed: Float)
  fun setTimer(timer: PlaybackTimer)
  fun clearTimer()

  enum class State {
    Disabled,
    Buffering,
    Playing,
    Paused,
  }
}

private val DefaultFadeDuration = 5.seconds
private const val DefaultFadeTickRate = 1000L / 16L
