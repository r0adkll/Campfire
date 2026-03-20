package app.campfire.audioplayer.impl.history

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.OnFinishedListener
import app.campfire.audioplayer.history.PlaybackAction
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import app.campfire.core.di.ComponentHolder
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.session.userId
import app.campfire.core.time.FatherTime
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A decorator around [AudioPlayer] that records user-initiated playback actions
 * via [PlaybackHistoryRecorder].
 */
class RecordingAudioPlayer(
  private val delegate: AudioPlayer,
  private val recorder: PlaybackHistoryRecorder,
  private val fatherTime: FatherTime,
  private val scope: CoroutineScope,
) : AudioPlayer {

  // region Delegated properties

  override val preparedSession get() = delegate.preparedSession
  override val state: StateFlow<AudioPlayer.State> get() = delegate.state
  override val overallTime: StateFlow<Duration> get() = delegate.overallTime
  override val currentTime: StateFlow<Duration> get() = delegate.currentTime
  override val currentDuration: StateFlow<Duration> get() = delegate.currentDuration
  override val currentMetadata: StateFlow<Metadata> get() = delegate.currentMetadata
  override val playbackSpeed: StateFlow<Float> get() = delegate.playbackSpeed
  override val runningTimer: StateFlow<RunningTimer?> get() = delegate.runningTimer

  // endregion

  // region Delegated methods (no recording)

  override suspend fun prepare(
    session: app.campfire.core.model.Session,
    playImmediately: Boolean,
    chapterId: Int?,
    onFinished: OnFinishedListener,
  ) = delegate.prepare(session, playImmediately, chapterId, onFinished)

  override fun release() = delegate.release()
  override fun stop() = delegate.stop()
  override fun setPlaybackSpeed(speed: Float) = delegate.setPlaybackSpeed(speed)
  override fun setTimer(timer: PlaybackTimer) = delegate.setTimer(timer)
  override fun clearTimer() = delegate.clearTimer()

  // endregion

  // region Recorded actions

  override fun pause() {
    val position = delegate.overallTime.value
    delegate.pause()
    record(PlaybackActionType.Pause, fromPosition = position, toPosition = position)
  }

  override fun fadeToPause(duration: Duration, tickRate: Long): Job {
    val position = delegate.overallTime.value
    record(PlaybackActionType.Pause, fromPosition = position, toPosition = position)
    return delegate.fadeToPause(duration, tickRate)
  }

  override fun playPause() {
    val position = delegate.overallTime.value
    val wasPaused = delegate.state.value != AudioPlayer.State.Playing
    delegate.playPause()
    record(
      type = if (wasPaused) PlaybackActionType.Play else PlaybackActionType.Pause,
      fromPosition = position,
      toPosition = position,
    )
  }

  override fun seekTo(itemIndex: Int) {
    val from = delegate.overallTime.value
    delegate.seekTo(itemIndex)
    val to = delegate.overallTime.value
    record(PlaybackActionType.Seek, fromPosition = from, toPosition = to)
  }

  override fun seekTo(progress: Float) {
    val from = delegate.overallTime.value
    delegate.seekTo(progress)
    val to = delegate.overallTime.value
    record(PlaybackActionType.Seek, fromPosition = from, toPosition = to)
  }

  override fun seekTo(timestamp: Duration) {
    val from = delegate.overallTime.value
    delegate.seekTo(timestamp)
    record(PlaybackActionType.Seek, fromPosition = from, toPosition = timestamp)
  }

  override fun skipToNext() {
    val from = delegate.overallTime.value
    delegate.skipToNext()
    val to = delegate.overallTime.value
    record(PlaybackActionType.SkipNext, fromPosition = from, toPosition = to)
  }

  override fun skipToPrevious() {
    val from = delegate.overallTime.value
    delegate.skipToPrevious()
    val to = delegate.overallTime.value
    record(PlaybackActionType.SkipPrevious, fromPosition = from, toPosition = to)
  }

  override fun seekForward() {
    val from = delegate.overallTime.value
    delegate.seekForward()
    val to = delegate.overallTime.value
    record(PlaybackActionType.SeekForward, fromPosition = from, toPosition = to)
  }

  override fun seekBackward() {
    val from = delegate.overallTime.value
    delegate.seekBackward()
    val to = delegate.overallTime.value
    record(PlaybackActionType.SeekBackward, fromPosition = from, toPosition = to)
  }

  // endregion

  private fun record(type: PlaybackActionType, fromPosition: Duration, toPosition: Duration) {
    val session = delegate.preparedSession ?: return
    val userId = ComponentHolder.component<PlaybackHistoryComponent>()
      .playbackHistoryUserSession
      .userId ?: return

    scope.launch {
      recorder.record(
        PlaybackAction(
          id = 0,
          libraryItemId = session.libraryItem.id,
          userId = userId,
          type = type,
          timestamp = fatherTime.now(),
          fromPosition = fromPosition,
          toPosition = toPosition,
        ),
      )
    }
  }
}
