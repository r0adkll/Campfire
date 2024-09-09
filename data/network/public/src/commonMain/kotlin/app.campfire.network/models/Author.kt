package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * An author object which includes a description and image path. The library items and series associated with the author are optionally included.
 *
 * @param id The ID of the author.
 * @param asin The Audible identifier (ASIN) of the author. Will be null if unknown. Not the Amazon identifier.
 * @param name The name of the author.
 * @param description The new description of the author.
 * @param imagePath The absolute path for the author image. This will be in the `metadata/` directory. Will be null if there is no image.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param libraryItems The items associated with the author
 * @param series The series associated with the author
 */
@Serializable
data class Author(
  val id: String,
  val asin: String? = null,
  val name: String,
  val libraryId: String,
  val description: String? = null,
  val imagePath: String? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val numBooks: Int? = null,

  // Attributes only included in /authors/:id endpoint
  val libraryItems: List<LibraryItemMinified<MinifiedBookMetadata>>? = null,
  val series: List<AuthorSeries>? = null,
)
