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

import app.campfire.network.models.BookMetadataMinified
import app.campfire.network.models.BookMinified

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Contextual

/**
 * The minified media of the library item.
 *
 * @param metadata
 * @param coverPath The absolute path on the server of the cover file. Will be null if there is no cover.
 * @param tags Tags applied to items.
 * @param numTracks The number of tracks the book's audio files have.
 * @param numAudioFiles The number of audio files the book has.
 * @param numChapters The number of chapters the book has.
 * @param numMissingParts The total number of missing parts the book has.
 * @param numInvalidAudioFiles The number of invalid audio files the book has.
 * @param duration The total length (in seconds) of the item or file.
 * @param propertySize The total size (in bytes) of the item or file.
 * @param ebookFormat The format of ebook of the book. Will be null if the book is an audiobook.
 */
@Serializable

data class MediaMinified (

    @SerialName(value = "metadata")
    val metadata: BookMetadataMinified? = null,

    /* The absolute path on the server of the cover file. Will be null if there is no cover. */
    @SerialName(value = "coverPath")
    val coverPath: kotlin.String? = null,

    /* Tags applied to items. */
    @SerialName(value = "tags")
    val tags: kotlin.collections.List<kotlin.String>? = null,

    /* The number of tracks the book's audio files have. */
    @SerialName(value = "numTracks")
    val numTracks: kotlin.Int? = null,

    /* The number of audio files the book has. */
    @SerialName(value = "numAudioFiles")
    val numAudioFiles: kotlin.Int? = null,

    /* The number of chapters the book has. */
    @SerialName(value = "numChapters")
    val numChapters: kotlin.Int? = null,

    /* The total number of missing parts the book has. */
    @SerialName(value = "numMissingParts")
    val numMissingParts: kotlin.Int? = null,

    /* The number of invalid audio files the book has. */
    @SerialName(value = "numInvalidAudioFiles")
    val numInvalidAudioFiles: kotlin.Int? = null,

    /* The total length (in seconds) of the item or file. */
    @SerialName(value = "duration")
    val duration: Double,

    /* The total size (in bytes) of the item or file. */
    @SerialName(value = "size")
    val propertySize: kotlin.Int? = null,

    /* The format of ebook of the book. Will be null if the book is an audiobook. */
    @SerialName(value = "ebookFormat")
    val ebookFormat: kotlin.String? = null

) {


}

