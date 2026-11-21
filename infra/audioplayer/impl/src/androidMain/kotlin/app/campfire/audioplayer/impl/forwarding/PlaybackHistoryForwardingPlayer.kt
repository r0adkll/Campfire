// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.forwarding

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.history.PlaybackHistoryRecorder
import app.campfire.audioplayer.impl.overallPosition
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

  /**
   * The session whose most recent Play has not been closed by a Pause yet. It outlives the
   * prepared session, so stopping a book to start another still records the old book's Pause.
   */
  private var playingSession: Session? = null

  /**
   * The book that played to its end, until a seek or a new queue moves off it. Tracked here rather
   * than read from `playbackState`, which a cast player may not update as soon as a seek returns.
   */
  private var endedSession: Session? = null

  /** Playback failed beyond recovery; nothing plays again until the player is re-prepared. */
  private var hasFailed = false

  /** False inside [withoutRecording]. */
  @PublishedApi
  internal var isRecording: Boolean = true

  init {
    wrappedPlayer.addListener(PlayerInitiatedStopListener())
  }

  override fun play() {
    val wasPlayWhenReady = playWhenReady
    super.play()
    recordPlayIfStarted(wasPlayWhenReady)
  }

  /**
   * Starting a session with autoplay ([app.campfire.audioplayer.AudioPlayer.prepare]) begins
   * playback through `playWhenReady` rather than [play], so record that too. The MediaSession
   * (notification, lock screen, Bluetooth, Auto) reaches this layer through
   * [ChapterWindowForwardingPlayer], a `ForwardingSimpleBasePlayer` that forwards `pause()` as
   * `setPlayWhenReady(false)`, so that is recorded as a Pause.
   */
  override fun setPlayWhenReady(playWhenReady: Boolean) {
    val wasPlayWhenReady = this.playWhenReady
    if (!playWhenReady && wasPlayWhenReady) recordPauseForPlayingSession()
    super.setPlayWhenReady(playWhenReady)
    if (playWhenReady) recordPlayIfStarted(wasPlayWhenReady)
  }

  /**
   * Records a Play unless the same session was already playing, since nothing changed. A new
   * session prepared while the player is already playing (switching books, queue auto-advance)
   * still counts as a start; re-preparing the same session (HLS fallback, cache recovery) does not.
   * Nothing plays from the end of a book until it seeks, or after a failure until the player is
   * re-prepared, so neither records a Play.
   */
  private fun recordPlayIfStarted(wasPlayWhenReady: Boolean) {
    val session = session() ?: return
    if (wasPlayWhenReady && session.id == playingSession?.id) return
    if (endedSession != null || hasFailed) return
    playingSession = session
    record(session, PlaybackActionType.Play, overallPosition.milliseconds)
  }

  override fun pause() {
    val wasPlayWhenReady = playWhenReady
    super.pause()
    if (wasPlayWhenReady) recordPauseForPlayingSession()
  }

  override fun stop() {
    if (playWhenReady) recordPauseForPlayingSession()
    super.stop()
  }

  override fun prepare() {
    hasFailed = false
    super.prepare()
  }

  /** Playback failed beyond recovery, which stops the book. */
  fun recordPlaybackFailed() {
    recordPauseForPlayingSession()
    hasFailed = true
  }

  /**
   * Runs [block] without recording history, for player changes the user didn't ask for (falling
   * back from a dead cast session). A pause inside still ends the playing book, silently. Inline,
   * so the block can suspend; anything the player reports meanwhile goes unrecorded too.
   */
  inline fun <T> withoutRecording(block: () -> T): T {
    val wasRecording = isRecording
    isRecording = false
    try {
      return block()
    } finally {
      isRecording = wasRecording
    }
  }

  override fun setMediaItems(mediaItems: List<MediaItem>) {
    onQueueReplaced()
    super.setMediaItems(mediaItems)
  }

  override fun setMediaItems(mediaItems: List<MediaItem>, resetPosition: Boolean) {
    onQueueReplaced()
    super.setMediaItems(mediaItems, resetPosition)
  }

  override fun setMediaItems(mediaItems: List<MediaItem>, startIndex: Int, startPositionMs: Long) {
    onQueueReplaced()
    super.setMediaItems(mediaItems, startIndex, startPositionMs)
  }

  override fun clearMediaItems() {
    onQueueReplaced()
    super.clearMediaItems()
  }

  /**
   * Preparing a session replaces the queue. Doing so while another book plays stops that book, so
   * record its Pause first, while the position still belongs to it. Re-preparing the session that
   * is playing (HLS fallback, cache recovery) doesn't stop anything. The new queue is neither at
   * its end nor failed.
   */
  private fun onQueueReplaced() {
    if (playWhenReady && session()?.id != playingSession?.id) recordPauseForPlayingSession()
    endedSession = null
    hasFailed = false
  }

  /** Records a Pause for the [playingSession], if one is playing, and closes it. */
  private fun recordPauseForPlayingSession() {
    val stopped = playingSession ?: return
    playingSession = null
    record(stopped, PlaybackActionType.Pause, overallPosition.milliseconds)
  }

  /** Playback stops that the player makes on its own, beneath the commands this layer sees. */
  private inner class PlayerInitiatedStopListener : Player.Listener {
    /**
     * Headphones unplugged / Bluetooth disconnected, another app taking audio focus for good, or
     * a pause from the cast receiver (its remote, or another device controlling it).
     */
    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
      if (!playWhenReady && reason in PLAYER_INITIATED_PAUSE_REASONS) recordPauseForPlayingSession()
    }

    /**
     * The book ran out. Finished is recorded for it, so it just stops counting as playing; a seek
     * that resumes it records a fresh Play (see [recordSeek]).
     */
    override fun onPlaybackStateChanged(playbackState: Int) {
      if (playbackState != Player.STATE_ENDED) return
      endedSession = playingSession ?: session()
      playingSession = null
    }
  }

  override fun seekForward() = recordSeek(
    type = PlaybackActionType.SeekForward,
    toPosition = { it + playbackSettings.forwardTimeMs.milliseconds },
  ) { super.seekForward() }

  override fun seekBack() = recordSeek(
    type = PlaybackActionType.SeekBackward,
    toPosition = { it - playbackSettings.backwardTimeMs.milliseconds },
  ) { super.seekBack() }

  override fun seekToNextMediaItem() = recordSeek(PlaybackActionType.SkipNext) { super.seekToNextMediaItem() }

  override fun seekToPreviousMediaItem() = recordSeek(PlaybackActionType.SkipPrevious) {
    super.seekToPreviousMediaItem()
  }

  override fun seekToNext() = recordSeek(PlaybackActionType.SkipNext) { super.seekToNext() }

  override fun seekToPrevious() = recordSeek(PlaybackActionType.SkipPrevious) { super.seekToPrevious() }

  override fun seekTo(positionMs: Long) = recordSeek(PlaybackActionType.Seek) { super.seekTo(positionMs) }

  /**
   * A seek from the very start is the audio player positioning a freshly prepared queue, not the
   * user, so it isn't recorded.
   */
  override fun seekTo(mediaItemIndex: Int, positionMs: Long) = recordSeek(
    type = PlaybackActionType.Seek,
    ignoreFromStart = true,
  ) { super.seekTo(mediaItemIndex, positionMs) }

  override fun seekToDefaultPosition() = recordSeek(PlaybackActionType.Seek) { super.seekToDefaultPosition() }

  /** A chapter tap; never recorded as a Seek, but it can still resume a book from its end. */
  override fun seekToDefaultPosition(mediaItemIndex: Int) = recordSeek(type = null) {
    super.seekToDefaultPosition(mediaItemIndex)
  }

  /**
   * Runs [seek] and records it as [type] (if any) against the current session. [toPosition]
   * defaults to where the seek landed. A seek that moves off the end of a book leaves the end, and
   * while the book is still set to play it resumes, so that also records a Play. Whether it moved
   * is read from the position, which players update as a seek returns; a skip past the last
   * chapter is ignored by the player and stays at the end.
   */
  private inline fun recordSeek(
    type: PlaybackActionType?,
    ignoreFromStart: Boolean = false,
    toPosition: (fromPosition: Duration) -> Duration = { overallPosition.milliseconds },
    seek: () -> Unit,
  ) {
    val fromPosition = overallPosition.milliseconds
    seek()
    val session = session() ?: return
    if (type != null && (!ignoreFromStart || fromPosition != Duration.ZERO)) {
      record(session, type, fromPosition, toPosition(fromPosition))
    }
    val ended = endedSession ?: return
    if (overallPosition.milliseconds == fromPosition) return
    endedSession = null
    if (playWhenReady && ended.id == session.id) recordPlayIfStarted(wasPlayWhenReady = false)
  }

  private fun record(
    session: Session,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration? = null,
  ) {
    if (!isRecording) return
    recorder.record(
      libraryItemId = session.libraryItem.id,
      type = type,
      fromPosition = fromPosition,
      toPosition = toPosition,
      episodeId = session.episodeId,
    )
  }

  private companion object {
    val PLAYER_INITIATED_PAUSE_REASONS = setOf(
      Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY,
      Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS,
      Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE,
    )
  }
}
