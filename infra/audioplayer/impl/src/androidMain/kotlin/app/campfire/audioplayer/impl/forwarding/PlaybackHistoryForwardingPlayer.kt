package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.impl.overallPosition
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.Session
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@UnstableApi
class PlaybackHistoryForwardingPlayer(
  player: Player,
  private val playbackSettings: PlaybackSettings,
  private val recorder: PlaybackHistoryRecorder,
  private val session: () -> Session?,
) : ForwardingPlayer(player) {

  private val libraryItemId: LibraryItemId?
    get() = session()?.libraryItem?.id

  override fun play() {
    super.play()
    libraryItemId?.let {
      val fromPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.Play,
        fromPosition = fromPosition,
      )
    }
  }

  override fun pause() {
    super.pause()
    libraryItemId?.let {
      val fromPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.Pause,
        fromPosition = fromPosition,
      )
    }
  }

  override fun seekForward() {
    val fromPosition = overallPosition.milliseconds
    super.seekForward()
    libraryItemId?.let {
      val seekForwardAmount = playbackSettings.forwardTimeMs.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SeekForward,
        fromPosition = fromPosition,
        toPosition = fromPosition + seekForwardAmount,
      )
    }
  }

  override fun seekBack() {
    val fromPosition = overallPosition.milliseconds
    super.seekBack()
    libraryItemId?.let {
      val seekForwardAmount = playbackSettings.forwardTimeMs.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SeekBackward,
        fromPosition = fromPosition,
        toPosition = fromPosition - seekForwardAmount,
      )
    }
  }

  override fun seekToNextMediaItem() {
    val fromPosition = overallPosition.milliseconds
    super.seekToNextMediaItem()
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SkipNext,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekToPreviousMediaItem() {
    val fromPosition = overallPosition.milliseconds
    super.seekToPreviousMediaItem()
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SkipPrevious,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekToNext() {
    val fromPosition = overallPosition.milliseconds
    super.seekToNext()
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SkipNext,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekToPrevious() {
    val fromPosition = overallPosition.milliseconds
    super.seekToPrevious()
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.SkipPrevious,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekTo(positionMs: Long) {
    val fromPosition = overallPosition.milliseconds
    super.seekTo(positionMs)
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.Seek,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
    val fromPosition = overallPosition.milliseconds
    super.seekTo(mediaItemIndex, positionMs)

    // If the 'from' position is zero, then we are likely preparing the audioplayer
    // and this action doesn't count as user-driven. So let's ignore it.
    if (fromPosition == Duration.ZERO) return
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.Seek,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }

  override fun seekToDefaultPosition() {
    val fromPosition = overallPosition.milliseconds
    super.seekToDefaultPosition()
    libraryItemId?.let {
      val toPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        type = PlaybackActionType.Seek,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }
}
