package app.campfire.network.models

import kotlinx.serialization.Serializable

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
data class MediaMinified(
  val id: String,
  val metadata: BookMetadataMinified,
  val coverPath: String,
  val tags: List<String>? = null,
  val numTracks: Int = 0,
  val numAudioFiles: Int = 0,
  val numChapters: Int = 0,
  val numMissingParts: Int = 0,
  val numInvalidAudioFiles: Int = 0,
  val duration: Double,
  val size: Long,
  val propertySize: Int? = null,
  val ebookFormat: String? = null,
)
