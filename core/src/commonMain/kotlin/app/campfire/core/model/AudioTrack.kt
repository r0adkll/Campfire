package app.campfire.core.model

data class AudioTrack(
  val index: Int,
  val startOffset: Float,
  val duration: Float,
  val title: String,
  val contentUrl: String,
  val contentUrlWithToken: String,
  val mimeType: String,
  val codec: String,
  val metadata: FileMetadata,
)
