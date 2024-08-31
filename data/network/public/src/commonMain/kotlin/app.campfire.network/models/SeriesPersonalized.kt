package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * A series object which includes the name and description of the series.
 *
 * @param id The ID of the series.
 * @param name The name of the series.
 * @param description A description for the series. Will be null if there is none.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 */
@Serializable
data class SeriesPersonalized(
  val id: String,
  val name: String,
  val description: String? = null,
  val addedAt: Long,
  val updatedAt: Long,

  val books: List<LibraryItemMinified<MinifiedBookMetadata>>? = null,
  val inProgress: Boolean? = null,
  val hasActiveBook: Boolean? = null,
  val hideFromContinueListening: Boolean? = null,
  val bookInProgressLastUpdate: Long? = null,
  val firstBookUnread: LibraryItemMinified<MinifiedBookMetadata>? = null,
)
