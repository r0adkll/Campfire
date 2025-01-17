package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Shelf {
  abstract val id: String
  abstract val label: String
  abstract val labelStringKey: String
  abstract val total: Int

  @Serializable
  @SerialName("book")
  data class BookShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified<MinifiedBookMetadata>>,
  ) : Shelf()

  @Serializable
  @SerialName("podcast")
  data class PodcastShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified<MinifiedBookMetadata>>,
  ) : Shelf()

  @Serializable
  @SerialName("episode")
  data class EpisodeShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<LibraryItemMinified<MinifiedBookMetadata>>,
  ) : Shelf()

  @Serializable
  @SerialName("series")
  data class SeriesShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<SeriesPersonalized>,
  ) : Shelf()

  @Serializable
  @SerialName("authors")
  data class AuthorShelf(
    override val id: String,
    override val label: String,
    override val labelStringKey: String,
    override val total: Int,
    val entities: List<Author>,
  ) : Shelf()
}
