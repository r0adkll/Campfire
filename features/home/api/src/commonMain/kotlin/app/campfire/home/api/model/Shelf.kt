package app.campfire.home.api.model

/**
 * A shelf of items or recommendations for the user's home screen.
 *
 * Entities can be:
 * * LibraryItemMinified
 * * Series
 * * Author
 */
data class Shelf<EntityT>(
  val id: String,
  val label: String,
  val total: Int,
  val entities: List<EntityT>,
)
