package app.campfire.home.ui

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Series
import app.campfire.core.model.SeriesId
import kotlin.time.Clock

/**
 * Creates a fake [Series] object for use in tests.
 */
fun series(
  id: SeriesId,
  name: String,
  description: String? = "A gripping saga of test data and mock objects.",
  addedAt: Long = Clock.System.now().toEpochMilliseconds(),
  updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
  books: List<LibraryItem>? = null,
  nameIgnorePrefix: String? = "The",
  nameIgnorePrefixSort: String? = "Fake Chronicles, The",
  totalDurationInMillis: Long? = 7200000, // 2 hours
  inProgress: Boolean = true,
  hasActiveBook: Boolean = true,
  hideFromContinueListening: Boolean = false,
  bookInProgressLastUpdate: Long? = Clock.System.now().toEpochMilliseconds(),
  firstBookUnreadId: LibraryItemId? = "book_2",
): Series {
  return Series(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    books = books,
    nameIgnorePrefix = nameIgnorePrefix,
    nameIgnorePrefixSort = nameIgnorePrefixSort,
    totalDurationInMillis = totalDurationInMillis,
    inProgress = inProgress,
    hasActiveBook = hasActiveBook,
    hideFromContinueListening = hideFromContinueListening,
    bookInProgressLastUpdate = bookInProgressLastUpdate,
    firstBookUnreadId = firstBookUnreadId,
  )
}
