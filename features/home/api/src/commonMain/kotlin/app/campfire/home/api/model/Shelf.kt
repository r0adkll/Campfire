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
