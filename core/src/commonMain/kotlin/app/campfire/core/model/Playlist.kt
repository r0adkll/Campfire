package app.campfire.core.model

import kotlinx.datetime.LocalDateTime

typealias PlaylistId = String

data class Playlist(
  val id: PlaylistId,
  val name: String,
  val description: String?,
  val lastUpdatedAt: LocalDateTime,
  val createdAt: LocalDateTime,

  // The API almost never returns a full playlist un-expanded. So if we are dealing in
  // whole playlists just assume the expanded formation. Minified are mainly used
  // for making API requests
  val items: List<Item.Expanded>,
) {

  interface Item {
    val index: Int
    val libraryItemId: String
    val episodeId: String?

    data class Minified(
      override val index: Int,
      override val libraryItemId: String,
      override val episodeId: String?,
    ) : Item

    data class Expanded(
      override val index: Int,
      override val libraryItemId: String,
      override val episodeId: String?,
      val libraryItem: LibraryItem,
    ) : Item {

      fun asMinified(): Minified = Minified(index, libraryItemId, episodeId)
    }
  }
}
