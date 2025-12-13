package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
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
