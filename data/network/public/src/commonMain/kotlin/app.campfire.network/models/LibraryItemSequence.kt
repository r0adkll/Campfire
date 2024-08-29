package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A single item on the server, like a book or podcast. Includes series sequence information.
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
 * @param sequence The position in the series the book is.
 */
@Serializable
data class LibraryItemSequence(
  val id: String,
  val oldLibraryItemId: String? = null,
  val ino: String? = null,
  val libraryId: String,
  val folderId: String,
  val path: String? = null,
  val relPath: String? = null,
  val isFile: Boolean? = null,
  val mtimeMs: Int? = null,
  val ctimeMs: Int? = null,
  val birthtimeMs: Int? = null,
  val addedAt: Int? = null,
  val updatedAt: Int? = null,
  val isMissing: Boolean? = null,
  val isInvalid: Boolean? = null,
  val mediaType: MediaType? = null,
  val sequence: String? = null,
)
