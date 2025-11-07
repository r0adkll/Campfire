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

infix fun Bookmark.isSame(other: Bookmark): Boolean {
  return userId == other.userId &&
    libraryItemId == other.libraryItemId &&
    time == other.time
}

infix fun Bookmark.isContentDifferent(other: Bookmark): Boolean {
  return isSame(other) && title != other.title
}
