package app.campfire.core.model

/**
 * A union interface to tightly define what entities can be returned from the personalized
 * home feed in a shelf.
 */
sealed interface ShelfEntity {

  /**
   * Entry shape used by `episodes-recently-added`-style shelves: a podcast [LibraryItem]
   * paired with the specific [recentEpisode] the shelf is highlighting. The libraryItem's
   * `media` is always [Media.Podcast]; it carries the full podcast metadata while the
   * episode carries the actual playable content for that shelf entry.
   */
  data class EpisodeShelfEntry(
    val libraryItem: LibraryItem,
    val recentEpisode: PodcastEpisode,
  ) : ShelfEntity {

    val transitionKey: String get() = libraryItem.id + recentEpisode.id
  }
}
