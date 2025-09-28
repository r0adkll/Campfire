package app.campfire.libraries.api

import app.campfire.core.model.Author
import app.campfire.core.model.AuthorId
import app.campfire.core.model.SeriesId

sealed interface LibraryItemFilter {
  val group: String
  val value: String

  class Genres(
    override val value: String,
  ) : LibraryItemFilter {
    override val group: String = "genres"
  }

  class Tags(
    override val value: String,
  ) : LibraryItemFilter {
    override val group: String = "tags"
  }

  class Series(
    override val value: SeriesId,
  ) : LibraryItemFilter {
    override val group: String = "series"
  }

  class Authors(
    val author: Author,
  ) : LibraryItemFilter {
    override val value: String = author.id
    override val group: String = "authors"
  }

  class Progress(
    val type: Type,
  ) : LibraryItemFilter {
    override val value: String = type.value
    override val group: String = "progress"

    enum class Type(val value: String) {
      Finished("finished"),
      NotStarted("not-started"),
      NotFinished("not-finished"),
      InProgress("in-progress"),
    }
  }

  class Narrators(
    override val value: String,
  ) : LibraryItemFilter {
    override val group: String = "narrators"
  }

  class Missing(
    val type: Type,
  ) : LibraryItemFilter {
    override val value: String = type.value
    override val group: String = "missing"

    enum class Type(val value: String) {
      ASIN("asin"),
      ISBN("isbn"),
      SUBTITLE("subtitle"),
      AUTHORS("authors"),
      PUBLISHED_YEAR("publishedYear"),
      SERIES("series"),
      DESCRIPTION("description"),
      GENRES("genres"),
      TAGS("tags"),
      NARRATORS("narrators"),
      PUBLISHER("publisher"),
      LANGUAGE("language"),
    }
  }

  class Languages(
    override val value: String,
  ) : LibraryItemFilter {
    override val group: String = "languages"
  }

  class Tracks(
    val type: Type,
  ) : LibraryItemFilter {
    override val value: String = type.value
    override val group: String = "tracks"

    enum class Type(val value: String) {
      Single("single"),
      Multi("multi"),
    }
  }
}
