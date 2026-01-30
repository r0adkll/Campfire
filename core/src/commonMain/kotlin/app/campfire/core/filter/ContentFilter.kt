package app.campfire.core.filter

import app.campfire.core.model.AuthorId
import app.campfire.core.model.SeriesId
import app.campfire.core.parcelize.Parcelable
import app.campfire.core.parcelize.Parcelize

@Parcelize
sealed interface ContentFilter : Parcelable {
  val group: String
  val value: String

  @Parcelize
  class Genres(
    override val value: String,
  ) : ContentFilter {
    override val group: String = "genres"
  }

  @Parcelize
  class Tags(
    override val value: String,
  ) : ContentFilter {
    override val group: String = "tags"
  }

  @Parcelize
  class Series(
    override val value: SeriesId,
    val seriesName: String,
  ) : ContentFilter {
    override val group: String = "series"
  }

  @Parcelize
  class Authors(
    val authorId: AuthorId,
    val authorName: String,
  ) : ContentFilter {
    override val value: String = authorId
    override val group: String = "authors"
  }

  @Parcelize
  class Progress(
    val type: Type,
  ) : ContentFilter {
    override val value: String = type.value
    override val group: String = "progress"

    enum class Type(val value: String) {
      Finished("finished"),
      NotStarted("not-started"),
      NotFinished("not-finished"),
      InProgress("in-progress"),
    }
  }

  @Parcelize
  class Narrators(
    override val value: String,
  ) : ContentFilter {
    override val group: String = "narrators"
  }

  @Parcelize
  class Publishers(
    override val value: String,
  ) : ContentFilter {
    override val group: String = "publishers"
  }

  @Parcelize
  class Missing(
    val type: Type,
  ) : ContentFilter {
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

  @Parcelize
  class Languages(
    override val value: String,
  ) : ContentFilter {
    override val group: String = "languages"
  }

  @Parcelize
  class Tracks(
    val type: Type,
  ) : ContentFilter {
    override val value: String = type.value
    override val group: String = "tracks"

    enum class Type(val value: String) {
      Single("single"),
      Multi("multi"),
    }
  }
}
