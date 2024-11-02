package app.campfire.audioplayer.model

import app.campfire.core.model.PlayMethod
import app.campfire.core.model.SessionId

/**
 * Represents a playback session that can be prepared by the [app.campfire.audioplayer.AudioPlayer] platform
 * implementation.
 */
class PlaybackSession(
  val id: SessionId,
  val playMethod: PlayMethod,
  val tracks: List<Track>,
  val token: String,
)
