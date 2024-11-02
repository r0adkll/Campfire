package app.campfire.audioplayer

import app.campfire.audioplayer.model.PlaybackSession

interface AudioPlayer {

  fun prepare(
    session: PlaybackSession,
    playWhenReady: Boolean,
    playbackRate: Float? = null,
  )
  fun pause()
  fun playPause()
  fun stop()
  fun seekTo(positionInMs: Long)

  fun skipToNext()
  fun skipToPrevious()

  fun seekForward(amount: Long? = null)
  fun seekBackward(amount: Long? = null)

  fun setPlaybackSpeed(speed: Float)
}
