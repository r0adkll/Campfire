package app.campfire.core.model

import kotlinx.datetime.LocalDateTime

typealias MediaShareId = String

data class MediaShare(
  val id: MediaShareId,
  val slug: String,
  val url: String,
  val libraryItemId: LibraryItemId,
  val isDownloadable: Boolean,
  val expiresAt: LocalDateTime,
  val createdAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)
