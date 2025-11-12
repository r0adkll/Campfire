package app.campfire.audioplayer.test

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAudioPlayer : AudioPlayer {

  val invocations = mutableListOf<Invocation>()

  override var preparedSession: Session? = null
  override val state = MutableStateFlow(AudioPlayer.State.Disabled)
  override val overallTime = MutableStateFlow(Duration.ZERO)
  override val currentTime = MutableStateFlow(Duration.ZERO)
  override val currentDuration = MutableStateFlow(Duration.ZERO)
  override val currentMetadata = MutableStateFlow(Metadata())
  override val playbackSpeed = MutableStateFlow(1f)
  override val runningTimer = MutableStateFlow(null)

  override suspend fun prepare(
    session: Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) {
    invocations += Invocation.Prepare(session, playImmediately, chapterId, onFinished)
  }

  override fun release() {
    invocations += Invocation.Release
  }

  override fun pause() {
    invocations += Invocation.Pause
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    invocations += Invocation.FadeToPause(duration, tickRate)
    return Job()
  }

  override fun playPause() {
    invocations += Invocation.PlayPause
  }

  override fun stop() {
    invocations += Invocation.Stop
  }

  override fun seekTo(itemIndex: Int) {
    invocations += Invocation.SeekTo(itemIndex)
  }

  override fun seekTo(progress: Float) {
    invocations += Invocation.SeekTo(progress)
  }

  override fun seekTo(timestamp: Duration) {
    invocations += Invocation.SeekTo(timestamp)
  }

  override fun skipToNext() {
    invocations += Invocation.SkipToNext
  }

  override fun skipToPrevious() {
    invocations += Invocation.SkipToPrevious
  }

  override fun seekForward() {
    invocations += Invocation.SeekForward
  }

  override fun seekBackward() {
    invocations += Invocation.SeekBackward
  }

  override fun setPlaybackSpeed(speed: Float) {
    invocations += Invocation.SetPlaybackSpeed(speed)
  }

  override fun setTimer(timer: PlaybackTimer) {
    invocations += Invocation.SetTimer(timer)
  }

  override fun clearTimer() {
    invocations += Invocation.ClearTimer
  }

  sealed interface Invocation {
    data class Prepare(
      val session: Session,
      val playImmediately: Boolean,
      val chapterId: Int?,
      val onFinished: OnFinishedListener,
    ) : Invocation
    data object Pause : Invocation
    data object PlayPause : Invocation
    data class FadeToPause(val duration: Duration, val tickRate: Long) : Invocation
    data object Stop : Invocation
    data object Release : Invocation
    data object SkipToNext : Invocation
    data object SkipToPrevious : Invocation
    data object SeekForward : Invocation
    data object SeekBackward : Invocation
    data class SeekTo(val value: Any) : Invocation
    data class SetPlaybackSpeed(val speed: Float) : Invocation
    data class SetTimer(val timer: PlaybackTimer) : Invocation
    data object ClearTimer : Invocation
  }
}
