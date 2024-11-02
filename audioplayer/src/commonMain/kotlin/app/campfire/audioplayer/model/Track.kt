package app.campfire.audioplayer.model

import app.campfire.core.model.Media

/**
 * Represents a hydrated [app.campfire.core.model.AudioTrack] item that can be interpreted by the underlying
 * [app.campfire.audioplayer.AudioPlayer] platform implementation.
 */
data class Track(
  val id: String,
  val contentUri: String,
  val mimeType: String,
  val metadata: Media.Metadata,
)
