package app.campfire.data.mapping

import app.campfire.account.api.CoverImageHydrator
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryItem
import app.campfire.data.Authors as DbAuthor
import app.campfire.data.SearchAuthors
import app.campfire.network.models.Author as NetworkAuthor

suspend fun NetworkAuthor.asDbModel(
  imageHydrator: CoverImageHydrator,
): DbAuthor {
  return DbAuthor(
    id = id,
    asin = asin,
    name = name,
    description = description,
    imagePath = imageHydrator.hydrateAuthor(id),
    addedAt = addedAt,
    updatedAt = updatedAt,
    numBooks = numBooks,
    libraryId = libraryId,
  )
}

fun DbAuthor.asDomainModel(
  items: List<LibraryItem> = emptyList(),
): Author {
  return Author(
    id = id,
    asin = asin,
    name = name,
    description = description,
    imagePath = imagePath,
    addedAt = addedAt,
    updatedAt = updatedAt,
    numBooks = numBooks,
    libraryItems = items,
  )
}

fun SearchAuthors.asDomainModel(): Author {
  return Author(
    id = id,
    asin = asin,
    name = name,
    description = description,
    imagePath = imagePath,
    addedAt = addedAt,
    updatedAt = updatedAt,
    numBooks = numBooks,
  )
}
