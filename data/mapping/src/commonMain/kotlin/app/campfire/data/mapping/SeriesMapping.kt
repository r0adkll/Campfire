package app.campfire.data.mapping

import app.campfire.network.models.Series as NetworkSeries
import app.campfire.data.Series as DbSeries
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series

fun NetworkSeries.asDbModel(libraryId: LibraryId): DbSeries {
  return DbSeries(
    id = id,
    name = name,
    description = description,
    addedAt = addedAt,
    updatedAt = updatedAt,
    libraryId = libraryId,
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
