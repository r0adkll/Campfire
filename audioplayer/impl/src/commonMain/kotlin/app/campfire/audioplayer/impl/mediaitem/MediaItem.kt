package app.campfire.audioplayer.impl.mediaitem

data class MediaItem(
  val id: String,
  val uri: String,
  val mimeType: String,
  val tag: String? = null,
  val metadata: Metadata? = null,
  val clipping: Clipping? = null,
) {

  data class Clipping(
    val startMs: Long,
    val endMs: Long,
  ) {
    val durationMs: Long get() = endMs - startMs
  }

  data class Metadata(
    val id: Int?,
    val title: String?,
    val artist: String?,
    val description: String,
    val subtitle: String?,
    val albumTitle: String?,
    val artworkUri: String?,
    val durationMs: Long?,
    val extras: Map<String, String>? = null,
  )
}
