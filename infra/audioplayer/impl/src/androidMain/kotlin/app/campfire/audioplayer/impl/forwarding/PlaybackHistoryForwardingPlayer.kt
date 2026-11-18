// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.impl.overallPosition
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import app.campfire.core.model.Session
import app.campfire.settings.api.PlaybackSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@UnstableApi
class PlaybackHistoryForwardingPlayer(
  player: Player,
  private val playbackSettings: PlaybackSettings,
  private val recorder: PlaybackHistoryRecorder,
  private val session: () -> Session?,
) : ForwardingPlayer(player) {

  private val libraryItemId: LibraryItemId?
    get() = session()?.libraryItem?.id

  private val episodeId: PodcastEpisodeId?
    get() = session()?.episodeId

  /** The session the most recent Play was recorded against. */
  private var lastPlaySessionId: Uuid? = null

  override fun play() {
    val wasPlayWhenReady = playWhenReady
    super.play()
    recordPlayIfStarted(wasPlayWhenReady)
  }

  /**
   * Starting a session with autoplay ([app.campfire.audioplayer.AudioPlayer.prepare]) begins
   * playback through `playWhenReady` rather than [play], so record that too.
   */
  override fun setPlayWhenReady(playWhenReady: Boolean) {
    val wasPlayWhenReady = this.playWhenReady
    super.setPlayWhenReady(playWhenReady)
    if (playWhenReady) recordPlayIfStarted(wasPlayWhenReady)
  }

  /**
   * Records a Play unless the same session was already playing, since nothing changed. A new
   * session prepared while the player is already playing (switching books, queue auto-advance)
   * still counts as a start; re-preparing the same session (HLS fallback, cache recovery) does not.
   */
  private fun recordPlayIfStarted(wasPlayWhenReady: Boolean) {
    val session = session() ?: return
    if (wasPlayWhenReady && session.id == lastPlaySessionId) return
    lastPlaySessionId = session.id
    recorder.record(
      libraryItemId = session.libraryItem.id,
      episodeId = session.episodeId,
      type = PlaybackActionType.Play,
      fromPosition = overallPosition.milliseconds,
    )
  }

  override fun pause() {
    super.pause()
    libraryItemId?.let {
      val fromPosition = overallPosition.milliseconds
      recorder.record(
        libraryItemId = it,
        episodeId = episodeId,
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
        episodeId = episodeId,
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
      val seekBackwardAmount = playbackSettings.backwardTimeMs.milliseconds
      recorder.record(
        libraryItemId = it,
        episodeId = episodeId,
        type = PlaybackActionType.SeekBackward,
        fromPosition = fromPosition,
        toPosition = fromPosition - seekBackwardAmount,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
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
        episodeId = episodeId,
        type = PlaybackActionType.Seek,
        fromPosition = fromPosition,
        toPosition = toPosition,
      )
    }
  }
}
