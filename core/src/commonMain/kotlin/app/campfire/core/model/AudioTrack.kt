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
  val metaTags: MetaTags?,
) {

  /**
   * Get the title of this track from its ID3 metadata tag, falling back
   * on its full title if not available
   */
  val taggedTitle: String
    get() = metaTags?.tagTitle ?: title
}

data class MetaTags(
  val tagAlbum: String? = null,
  val tagArtist: String? = null,
  val tagAlbumArtist: String? = null,
  val tagTitle: String? = null,
  val tagSubtitle: String? = null,
  val tagSeries: String? = null,
  val tagSeriesPart: String? = null,
  val tagTrack: String? = null,
)
