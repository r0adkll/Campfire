package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Represents an audio track with various properties.
 *
 * @param index The index of the audio track.
 * @param startOffset The start offset of the audio track in seconds.
 * @param duration The duration of the audio track in seconds.
 * @param title The title of the audio track.
 * @param contentUrl The URL where the audio track content is located.
 * @param mimeType The MIME type of the audio track.
 * @param codec The codec used for the audio track.
 * @param metadata
 */
@Serializable
data class AudioTrack(
  val index: Int,
  val startOffset: Float,
  val duration: Float,
  val title: String,
  val contentUrl: String,
  val mimeType: String,
  val codec: String,
  val metadata: FileMetadata,
)
