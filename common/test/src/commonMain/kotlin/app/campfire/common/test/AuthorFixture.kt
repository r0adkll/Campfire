package app.campfire.home.ui

import app.campfire.core.model.Author
import app.campfire.core.model.AuthorId
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Series
import kotlin.time.Clock

/**
 * Creates a fake [Author] object for use in tests.
 */
fun author(
  id: AuthorId,
  name: String,
  asin: String? = "B002BODS5I",
  description: String? = "An acclaimed author in the realm of imaginary tales and test data.",
  imagePath: String? = "/path/to/fake/author/image.jpg",
  addedAt: Long = Clock.System.now().toEpochMilliseconds(),
  updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
  numBooks: Int? = 5,
  libraryItems: List<LibraryItem> = emptyList(),
  series: List<Series> = emptyList(),
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
    libraryItems = libraryItems,
    series = series,
  )
}
