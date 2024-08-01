/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport"
)

package app.campfire.network.models


import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

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

data class AudioMetaTags (

    @SerialName(value = "tagAlbum")
    val tagAlbum: kotlin.String? = null,

    @SerialName(value = "tagArtist")
    val tagArtist: kotlin.String? = null,

    @SerialName(value = "tagGenre")
    val tagGenre: kotlin.String? = null,

    @SerialName(value = "tagTitle")
    val tagTitle: kotlin.String? = null,

    @SerialName(value = "tagSeries")
    val tagSeries: kotlin.String? = null,

    @SerialName(value = "tagSeriesPart")
    val tagSeriesPart: kotlin.String? = null,

    @SerialName(value = "tagTrack")
    val tagTrack: kotlin.String? = null,

    @SerialName(value = "tagDisc")
    val tagDisc: kotlin.String? = null,

    @SerialName(value = "tagSubtitle")
    val tagSubtitle: kotlin.String? = null,

    @SerialName(value = "tagAlbumArtist")
    val tagAlbumArtist: kotlin.String? = null,

    @SerialName(value = "tagDate")
    val tagDate: kotlin.String? = null,

    @SerialName(value = "tagComposer")
    val tagComposer: kotlin.String? = null,

    @SerialName(value = "tagPublisher")
    val tagPublisher: kotlin.String? = null,

    @SerialName(value = "tagComment")
    val tagComment: kotlin.String? = null,

    @SerialName(value = "tagDescription")
    val tagDescription: kotlin.String? = null,

    @SerialName(value = "tagEncoder")
    val tagEncoder: kotlin.String? = null,

    @SerialName(value = "tagEncodedBy")
    val tagEncodedBy: kotlin.String? = null,

    @SerialName(value = "tagIsbn")
    val tagIsbn: kotlin.String? = null,

    @SerialName(value = "tagLanguage")
    val tagLanguage: kotlin.String? = null,

    @SerialName(value = "tagASIN")
    val tagASIN: kotlin.String? = null,

    @SerialName(value = "tagOverdriveMediaMarker")
    val tagOverdriveMediaMarker: kotlin.String? = null,

    @SerialName(value = "tagOriginalYear")
    val tagOriginalYear: kotlin.String? = null,

    @SerialName(value = "tagReleaseCountry")
    val tagReleaseCountry: kotlin.String? = null,

    @SerialName(value = "tagReleaseType")
    val tagReleaseType: kotlin.String? = null,

    @SerialName(value = "tagReleaseStatus")
    val tagReleaseStatus: kotlin.String? = null,

    @SerialName(value = "tagISRC")
    val tagISRC: kotlin.String? = null,

    @SerialName(value = "tagMusicBrainzTrackId")
    val tagMusicBrainzTrackId: kotlin.String? = null,

    @SerialName(value = "tagMusicBrainzAlbumId")
    val tagMusicBrainzAlbumId: kotlin.String? = null,

    @SerialName(value = "tagMusicBrainzAlbumArtistId")
    val tagMusicBrainzAlbumArtistId: kotlin.String? = null,

    @SerialName(value = "tagMusicBrainzArtistId")
    val tagMusicBrainzArtistId: kotlin.String? = null

) {


}

