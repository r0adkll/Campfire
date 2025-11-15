package app.campfire.data.mapping

import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import app.campfire.data.SearchSeries
import app.campfire.data.SelectByShelfId
import app.campfire.data.Series as DbSeries
import app.campfire.network.models.Series as NetworkSeries
import app.campfire.network.models.SeriesPersonalized

fun NetworkSeries.asDbModel(libraryId: LibraryId): DbSeries {
  return DbSeries(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    libraryId = libraryId,
    inProgress = false,
    hasActiveBook = false,
    hideFromContinueListening = false,
    bookInProgressLastUpdate = null,
    firstBookUnreadId = null,
  )
}

fun SeriesPersonalized.asDbModel(libraryId: LibraryId): DbSeries {
  return DbSeries(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    libraryId = libraryId,
    inProgress = inProgress == true,
    hasActiveBook = hasActiveBook == true,
    hideFromContinueListening = hideFromContinueListening == true,
    bookInProgressLastUpdate = bookInProgressLastUpdate,
    firstBookUnreadId = firstBookUnread?.id,
  )
}

fun DbSeries.asDomainModel(
  books: List<LibraryItem>? = null,
): Series {
  return Series(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    books = books,
  )
}

fun SelectByShelfId.asDomainModel(
  books: List<LibraryItem>? = null,
): Series {
  return Series(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    inProgress = inProgress,
    hasActiveBook = hasActiveBook,
    hideFromContinueListening = hideFromContinueListening,
    bookInProgressLastUpdate = bookInProgressLastUpdate,
    firstBookUnreadId = firstBookUnreadId,
    books = books,
  )
}

fun SearchSeries.asDomainModel(
  books: List<LibraryItem>? = null,
): Series {
  return Series(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    books = books,
  )
}
