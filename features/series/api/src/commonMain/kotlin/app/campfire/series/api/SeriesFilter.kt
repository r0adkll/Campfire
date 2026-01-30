package app.campfire.series.api

import app.campfire.core.model.AuthorId
import app.campfire.core.parcelize.Parcelable
import app.campfire.core.parcelize.Parcelize

@Parcelize
sealed interface SeriesFilter : Parcelable {
  val group: String
  val value: String

  @Parcelize
  class Genres(
    override val value: String,
  ) : SeriesFilter {
    override val group: String = "genres"
  }

  @Parcelize
  class Tags(
    override val value: String,
  ) : SeriesFilter {
    override val group: String = "tags"
  }

  @Parcelize
  class Authors(
    val authorId: AuthorId,
    val authorName: String,
  ) : SeriesFilter {
    override val value: String = authorId
    override val group: String = "authors"
  }

  @Parcelize
  class Progress(
    val type: Type,
  ) : SeriesFilter {
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
  ) : SeriesFilter {
    override val group: String = "narrators"
  }

  @Parcelize
  class Publishers(
    override val value: String,
  ) : SeriesFilter {
    override val group: String = "publishers"
  }

  @Parcelize
  class Languages(
    override val value: String,
  ) : SeriesFilter {
    override val group: String = "languages"
  }
}
