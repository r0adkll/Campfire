package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Series and the included library items that an author has written.
 *
 * @param id The ID of the series.
 * @param name The name of the series.
 * @param items The items in the series. Each library item's media's metadata will have a `series` attribute, a `Series Sequence`, which is the matching series.
 */
@Serializable
data class AuthorSeries(
  val id: String,
  val name: String,
  val items: List<LibraryItemMinified<MinifiedBookMetadata>>? = null,
)
