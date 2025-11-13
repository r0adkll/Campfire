package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class LibraryItemExpanded(
  override val id: String,
  override val ino: String,
  override val libraryId: String,
  override val oldLibraryItemId: String? = null,
  override val folderId: String,
  override val path: String,
  override val relPath: String,
  override val isFile: Boolean,
  override val mtimeMs: Long,
  override val ctimeMs: Long,
  override val birthtimeMs: Long,
  override val addedAt: Long,
  override val updatedAt: Long,
  override val isMissing: Boolean,
  override val isInvalid: Boolean,
  override val mediaType: MediaType,
  // This shouldn't be nullable, but sometimes the API (i.e. Search) returns unexpected values
  override val size: Long? = null,
  override val numFiles: Int? = null,
  val media: MediaExpanded,
  val libraryFiles: List<LibraryFile>,
  val userMediaProgress: MediaProgress? = null,
) : LibraryItemBase()
