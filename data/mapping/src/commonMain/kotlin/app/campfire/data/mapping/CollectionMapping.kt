package app.campfire.data.mapping

import app.campfire.account.api.TokenHydrator
import app.campfire.core.model.Collection
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.UserId
import app.campfire.data.Collections as DbCollection
import app.campfire.network.models.Collection as NetworkCollection

fun NetworkCollection.asDbModel(
  userId: UserId,
): DbCollection {
  return DbCollection(
    id = id,
    name = name,
    description = description,
    cover = cover,
    coverFullPath = coverFullPath,
    updatedAt = lastUpdate,
    createdAt = createdAt,
    userId = userId,
    libraryId = libraryId,
  )
}

suspend fun NetworkCollection.asDomainModel(tokenHydrator: TokenHydrator): Collection {
  return Collection(
    id = id,
    name = name,
    description = description,
    cover = cover,
    coverFullPath = coverFullPath,
    updatedAt = lastUpdate,
    createdAt = createdAt,
    books = books.map { it.asDomainModel(tokenHydrator) },
  )
}

fun Collection.asDbModel(
  userId: UserId,
  libraryId: LibraryId,
): DbCollection {
  return DbCollection(
    id = id,
    name = name,
    description = description,
    cover = cover,
    coverFullPath = coverFullPath,
    updatedAt = updatedAt,
    createdAt = createdAt,
    libraryId = libraryId,
    userId = userId,
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
