package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
abstract class LibraryItemBase : NetworkModel() {
  abstract val id: String
  abstract val ino: String
  abstract val libraryId: String
  abstract val oldLibraryItemId: String?
  abstract val folderId: String
  abstract val path: String
  abstract val relPath: String
  abstract val isFile: Boolean
  abstract val mtimeMs: Long
  abstract val ctimeMs: Long
  abstract val birthtimeMs: Long
  abstract val addedAt: Long
  abstract val updatedAt: Long
  abstract val isMissing: Boolean
  abstract val isInvalid: Boolean
  abstract val mediaType: MediaType
  abstract val numFiles: Int?
  abstract val size: Long?
}
