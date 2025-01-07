package app.campfire.data.mapping

import app.campfire.core.model.Bookmark
import app.campfire.data.Bookmarks as DbBookmark
import app.campfire.network.models.AudioBookmark as NetworkBookmark
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun NetworkBookmark.asDbModel(userId: String): DbBookmark {
  return DbBookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    timeInSeconds = time,
    createdAt = Instant.fromEpochSeconds(createdAt).toLocalDateTime(TimeZone.UTC),
  )
}

fun NetworkBookmark.asDomainModel(userId: String): Bookmark {
  return Bookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    time = time.seconds,
    createdAt = Instant.fromEpochSeconds(createdAt).toLocalDateTime(TimeZone.UTC),
  )
}

fun DbBookmark.asDomainModel(): Bookmark {
  return Bookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    time = timeInSeconds.seconds,
    createdAt = createdAt,
  )
}

fun Bookmark.asDbModel(): DbBookmark {
  return DbBookmark(
    userId = userId,
    libraryItemId = libraryItemId,
    title = title,
    timeInSeconds = time.inWholeSeconds.toInt(),
    createdAt = createdAt,
  )
}
