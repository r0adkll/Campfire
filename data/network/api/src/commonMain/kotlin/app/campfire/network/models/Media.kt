package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The base information for every book-shaped media item from the server.
 *
 * Podcasts use the parallel [Podcast] network type, which is NOT a subclass of this — the field
 * sets are too different to share. The two are siblings under [LibraryItemMinified] / [LibraryItemExpanded].
 */
@Serializable
abstract class Media {
  abstract val id: String
  abstract val coverPath: String?
  abstract val tags: List<String>?
  abstract val numTracks: Int
  abstract val numAudioFiles: Int
  abstract val numChapters: Int
  abstract val numMissingParts: Int
  abstract val numInvalidAudioFiles: Int

  // These fields _should_ be non-null, but from some endpoints they are missing (i.e. Search)
  // so we must null-ify them, and attempt post-Network processing to determine if missing
  abstract val duration: Double?
  abstract val size: Long?

  abstract val propertySize: Int?
  abstract val ebookFormat: String?
}

@Serializable
data class MediaMinified(
  override val id: String,
  override val coverPath: String?,
  override val tags: List<String>? = null,
  override val numTracks: Int = 0,
  override val numAudioFiles: Int = 0,
  override val numChapters: Int = 0,
  override val numMissingParts: Int = 0,
  override val numInvalidAudioFiles: Int = 0,
  override val duration: Double? = null,
  override val size: Long? = null,
  override val propertySize: Int? = null,
  override val ebookFormat: String? = null,
  val metadata: BookMetadata,
) : Media()

@Serializable
data class MediaExpanded(
  override val id: String,
  override val coverPath: String?,
  override val tags: List<String>? = null,
  override val numTracks: Int = 0,
  override val numAudioFiles: Int = 0,
  override val numChapters: Int = 0,
  override val numMissingParts: Int = 0,
  override val numInvalidAudioFiles: Int = 0,
  override val duration: Double? = null,
  override val size: Long? = null,
  override val propertySize: Int? = null,
  override val ebookFormat: String? = null,
  val metadata: ExpandedBookMetadata,
  val audioFiles: List<AudioFile> = emptyList(),
  val chapters: List<BookChapter> = emptyList(),
  val tracks: List<AudioTrack> = emptyList(),
) : Media()
