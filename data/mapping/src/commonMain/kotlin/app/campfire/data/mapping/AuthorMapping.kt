package app.campfire.data.mapping

import app.campfire.account.api.CoverImageHydrator
import app.campfire.data.Authors as DbAuthor
import app.campfire.network.models.Author as NetworkAuthor
import app.campfire.core.model.Author
import app.campfire.core.model.LibraryId

suspend fun NetworkAuthor.asDbModel(
  libraryId: LibraryId,
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

fun DbAuthor.asDomainModel(): Author {
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
