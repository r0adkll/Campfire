package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * ID3 metadata tags pulled from the audio file on import. Only non-null tags will be returned in requests.
 *
 * @param tagAlbum
 * @param tagArtist
 * @param tagGenre
 * @param tagTitle
 * @param tagSeries
 * @param tagSeriesPart
 * @param tagTrack
 * @param tagDisc
 * @param tagSubtitle
 * @param tagAlbumArtist
 * @param tagDate
 * @param tagComposer
 * @param tagPublisher
 * @param tagComment
 * @param tagDescription
 * @param tagEncoder
 * @param tagEncodedBy
 * @param tagIsbn
 * @param tagLanguage
 * @param tagASIN
 * @param tagOverdriveMediaMarker
 * @param tagOriginalYear
 * @param tagReleaseCountry
 * @param tagReleaseType
 * @param tagReleaseStatus
 * @param tagISRC
 * @param tagMusicBrainzTrackId
 * @param tagMusicBrainzAlbumId
 * @param tagMusicBrainzAlbumArtistId
 * @param tagMusicBrainzArtistId
 */
@Serializable
data class AudioMetaTags(
  val tagAlbum: String? = null,
  val tagArtist: String? = null,
  val tagGenre: String? = null,
  val tagTitle: String? = null,
  val tagSeries: String? = null,
  val tagSeriesPart: String? = null,
  val tagTrack: String? = null,
  val tagDisc: String? = null,
  val tagSubtitle: String? = null,
  val tagAlbumArtist: String? = null,
  val tagDate: String? = null,
  val tagComposer: String? = null,
  val tagPublisher: String? = null,
  val tagComment: String? = null,
  val tagDescription: String? = null,
  val tagEncoder: String? = null,
  val tagEncodedBy: String? = null,
  val tagIsbn: String? = null,
  val tagLanguage: String? = null,
  val tagASIN: String? = null,
  val tagOverdriveMediaMarker: String? = null,
  val tagOriginalYear: String? = null,
  val tagReleaseCountry: String? = null,
  val tagReleaseType: String? = null,
  val tagReleaseStatus: String? = null,
  val tagISRC: String? = null,
  val tagMusicBrainzTrackId: String? = null,
  val tagMusicBrainzAlbumId: String? = null,
  val tagMusicBrainzAlbumArtistId: String? = null,
  val tagMusicBrainzArtistId: String? = null,
)
