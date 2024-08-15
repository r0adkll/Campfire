package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A single item on the server, like a book or podcast. Minified media format.
 *
 * @param id The ID of library items after 2.3.0.
 * @param oldLibraryItemId The ID of library items on server version 2.2.23 and before.
 * @param ino The inode of the item in the file system.
 * @param libraryId The ID of the library.
 * @param folderId The ID of the folder.
 * @param path The path of the library item on the server.
 * @param relPath The path, relative to the library folder, of the library item.
 * @param isFile Whether the library item is a single file in the root of the library folder.
 * @param mtimeMs The time (in ms since POSIX epoch) when the library item was last modified on disk.
 * @param ctimeMs The time (in ms since POSIX epoch) when the library item status was changed on disk.
 * @param birthtimeMs The time (in ms since POSIX epoch) when the library item was created on disk. Will be 0 if unknown.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param isMissing Whether the library item was scanned and no longer exists.
 * @param isInvalid Whether the library item was scanned and no longer has media files.
 * @param mediaType
 * @param media
 */
@Serializable
data class LibraryItemMinified(
  val id: String,
  val ino: String,
  val libraryId: String,
  val oldLibraryItemId: String? = null,
  val folderId: String,
  val path: String,
  val relPath: String,
  val isFile: Boolean,
  val mtimeMs: Long,
  val ctimeMs: Long,
  val birthtimeMs: Long,
  val addedAt: Long,
  val updatedAt: Long,
  val isMissing: Boolean,
  val isInvalid: Boolean,
  val mediaType: MediaType,
  val media: MediaMinified,
  val numFiles: Int,
  val size: Long,
)
