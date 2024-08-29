package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * An audio file for a book. Includes audio metadata and track numbers.
 *
 * @param index The index of the audio file.
 * @param ino The inode of the item in the file system.
 * @param metadata
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param trackNumFromMeta The track number of the audio file as pulled from the file's metadata. Will be null if unknown.
 * @param discNumFromMeta The disc number of the audio file as pulled from the file's metadata. Will be null if unknown.
 * @param trackNumFromFilename The track number of the audio file as determined from the file's name. Will be null if unknown.
 * @param discNumFromFilename The disc number of the audio file as determined from the file's name. Will be null if unknown.
 * @param manuallyVerified Whether the audio file has been manually verified by a user.
 * @param invalid Whether the audio file is missing from the server.
 * @param exclude Whether the audio file has been marked for exclusion.
 * @param error Any error with the audio file. Will be null if there is none.
 * @param format The format of the audio file.
 * @param duration The total length (in seconds) of the item or file.
 * @param bitRate The bit rate (in bit/s) of the audio file.
 * @param language The language of the audio file.
 * @param codec The codec of the audio file.
 * @param timeBase The time base of the audio file.
 * @param channels The number of channels the audio file has.
 * @param channelLayout The layout of the audio file's channels.
 * @param chapters If the audio file is part of an audiobook, the chapters the file contains.
 * @param embeddedCoverArt The type of embedded cover art in the audio file. Will be null if none exists.
 * @param metaTags
 * @param mimeType The MIME type of the audio file.
 */
@Serializable
data class AudioFile(
  val index: Int,
  val ino: String,
  val metadata: FileMetadata,
  val addedAt: Long,
  val updatedAt: Long,
  val trackNumFromMeta: Int? = null,
  val discNumFromMeta: Int? = null,
  val trackNumFromFilename: Int? = null,
  val discNumFromFilename: Int? = null,
  val manuallyVerified: Boolean,
  val invalid: Boolean? = null,
  val exclude: Boolean,
  val error: String? = null,
  val format: String,
  val duration: Float,
  val bitRate: Int,
  val language: String? = null,
  val codec: String,
  val timeBase: String,
  val channels: Int,
  val channelLayout: String,
  val chapters: List<BookChapter>,
  val embeddedCoverArt: String? = null,
  val metaTags: AudioMetaTags,
  val mimeType: String,
)
