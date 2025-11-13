package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A series object which includes the name and books in the series.
 *
 * @param id The ID of the series.
 * @param name The name of the series.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param nameIgnorePrefix The name of the series with any prefix moved to the end.
 * @param nameIgnorePrefixSort The name of the series with any prefix removed.
 * @param type Will always be `series`.
 * @param books The library items that contain the books in the series. A sequence attribute that denotes the position in the series the book is in, is tacked on.
 * @param totalDuration The combined duration (in seconds) of all books in the series.
 */
@Serializable
data class SeriesBooks(
  val id: String,
  val name: String? = null,
  val addedAt: Int? = null,
  val nameIgnorePrefix: String? = null,
  val nameIgnorePrefixSort: String? = null,
  val type: String? = null,
  val books: List<LibraryItemMinified<MinifiedBookMetadata>>? = null,
  val totalDuration: Double,
)
