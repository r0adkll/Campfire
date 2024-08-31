package app.campfire.data.mapping

import app.campfire.network.models.Collection as NetworkCollection
import app.campfire.data.Collections as DbCollection
import app.campfire.core.model.Collection
import app.campfire.core.model.LibraryItem

fun NetworkCollection.asDbModel(): DbCollection {
  return DbCollection(
    id = id,
    name = name,
    description = description,
    cover = cover,
    coverFullPath = coverFullPath,
    updatedAt = lastUpdate,
    createdAt = createdAt,
    libraryId = libraryId,
  )
}

fun DbCollection.asDomainModel(
  books: List<LibraryItem>,
): Collection {
  return Collection(
    id = id,
    name = name,
    description = description,
    cover = cover,
    coverFullPath = coverFullPath,
    books = books,
    createdAt = createdAt,
    updatedAt = updatedAt,
  )
}
