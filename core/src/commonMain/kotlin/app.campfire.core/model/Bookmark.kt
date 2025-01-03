package app.campfire.core.model

import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

data class Bookmark(
  val userId: UserId,
  val libraryItemId: LibraryItemId,
  val title: String,
  val time: Duration,
  val createdAt: LocalDateTime,
)
