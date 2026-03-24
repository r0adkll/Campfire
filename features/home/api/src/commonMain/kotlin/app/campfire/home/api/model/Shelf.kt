package app.campfire.home.api.model

import app.campfire.core.model.ShelfType

typealias ShelfId = String

/**
 * Represents a "shelf" of items in the user's personalized home feed.
 * https://api.audiobookshelf.org/#get-a-library-39-s-personalized-view
 */
data class Shelf(
  val id: ShelfId,
  val label: String,
  val total: Int,
  val type: ShelfType,
  val order: Int,
)

/**
 * A concret list of shelf-ids one can expect in the personalized home feed
 * response.
 */
@Suppress("ConstPropertyName")
object ShelfIds {
  const val ContinueListening = "continue-listening"
  const val ContinueSeries = "continue-series"
  const val ListenAgain = "listen-again"
  const val EpisodesRecentlyAdded = "episodes-recently-added"
  const val Recommended = "recommended"
  const val RecentlyAdded = "recently-added"
  const val RecentSeries = "recent-series"
  const val Discover = "discover"
  const val NewestAuthors = "newest-authors"
}
