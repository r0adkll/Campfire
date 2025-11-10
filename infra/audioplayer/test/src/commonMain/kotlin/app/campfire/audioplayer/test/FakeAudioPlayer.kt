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
    TODO("Not yet implemented")
  }

  override fun release() {
    TODO("Not yet implemented")
  }

  override fun pause() {
    TODO("Not yet implemented")
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    TODO("Not yet implemented")
  }

  override fun playPause() {
    TODO("Not yet implemented")
  }

  override fun stop() {
    TODO("Not yet implemented")
  }

  override fun seekTo(itemIndex: Int) {
    TODO("Not yet implemented")
  }

  override fun seekTo(progress: Float) {
    TODO("Not yet implemented")
  }

  override fun seekTo(timestamp: Duration) {
    TODO("Not yet implemented")
  }

  override fun skipToNext() {
    TODO("Not yet implemented")
  }

  override fun skipToPrevious() {
    TODO("Not yet implemented")
  }

  override fun seekForward() {
    TODO("Not yet implemented")
  }

  override fun seekBackward() {
    TODO("Not yet implemented")
  }

  override fun setPlaybackSpeed(speed: Float) {
    TODO("Not yet implemented")
  }

  override fun setTimer(timer: PlaybackTimer) {
    TODO("Not yet implemented")
  }

  override fun clearTimer() {
    TODO("Not yet implemented")
  }
}
