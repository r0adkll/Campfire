package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * The metadata for a file, including the path, size, and unix timestamps of the file.
 *
 * @param filename The filename of the file.
 * @param ext The file extension of the file.
 * @param path The absolute path on the server of the file.
 * @param relPath The path of the file, relative to the book's or podcast's folder.
 * @param propertySize The total size (in bytes) of the item or file.
 * @param mtimeMs The time (in ms since POSIX epoch) when the file was last modified on disk.
 * @param ctimeMs The time (in ms since POSIX epoch) when the file status was changed on disk.
 * @param birthtimeMs The time (in ms since POSIX epoch) when the file was created on disk. Will be 0 if unknown.
 */
@Serializable
data class FileMetadata(
  val filename: String,
  val ext: String,
  val path: String,
  val relPath: String,
  val size: Long,
  val mtimeMs: Long,
  val ctimeMs: Long,
  val birthtimeMs: Long,
)
