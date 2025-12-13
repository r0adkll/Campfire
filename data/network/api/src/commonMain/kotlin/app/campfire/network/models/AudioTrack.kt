package app.campfire.network.models

import kotlinx.serialization.Serializable

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
  val metaTags: MetaTags? = null,
)
