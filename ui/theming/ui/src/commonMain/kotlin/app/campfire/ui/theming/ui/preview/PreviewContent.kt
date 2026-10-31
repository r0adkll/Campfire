// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.theming.ui.preview

import androidx.compose.runtime.Immutable
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.preview.libraryItem
import app.campfire.core.model.preview.mediaProgress
import app.campfire.core.settings.ContentSortMode
import app.campfire.core.settings.ItemDisplayState
import app.campfire.core.settings.SortDirection

/**
 * What the theme preview draws: the user's own books, and whether the player takes its colors
 * from the cover of what's playing, as it does by default, instead of from the theme.
 */
@Immutable
data class PreviewContent(
  val library: PreviewLibrary = PreviewLibrary.Samples,
  val playerUsesCoverColors: Boolean = true,
)

/**
 * The books in the preview's Home and Library scenes. [Samples] stands in until the user's own
 * shelves load, and when they're empty.
 */
@Immutable
data class PreviewLibrary(
  val continueListening: PreviewShelf,
  val recentlyAdded: PreviewShelf,
  val itemCount: Int?,
  val isPodcastLibrary: Boolean = false,
  val displayState: ItemDisplayState = ItemDisplayState.Grid,
  val sortMode: ContentSortMode = ContentSortMode.Title,
  val sortDirection: SortDirection = SortDirection.Ascending,
) {
  /** The book the preview's mini player and player show. */
  val nowPlaying: PreviewShelfItem?
    get() = continueListening.items.firstOrNull() ?: recentlyAdded.items.firstOrNull()

  /** Every book on either shelf once, for the Library scene's grid. */
  val allItems: List<PreviewShelfItem>
    get() = (continueListening.items + recentlyAdded.items).distinctBy { it.key }

  val isEmpty: Boolean
    get() = continueListening.items.isEmpty() && recentlyAdded.items.isEmpty()

  companion object {
    val Samples = PreviewLibrary(
      continueListening = PreviewShelf(
        label = null,
        items = listOf(
          sample("sample_moby_dick", "Moby-Dick", "Herman Melville", progress = 0.62f),
          sample("sample_pride", "Pride and Prejudice", "Jane Austen", progress = 0.24f),
          sample("sample_dracula", "Dracula", "Bram Stoker", progress = 0.81f),
        ),
      ),
      recentlyAdded = PreviewShelf(
        label = null,
        items = listOf(
          sample("sample_frankenstein", "Frankenstein", "Mary Shelley"),
          sample("sample_time_machine", "The Time Machine", "H. G. Wells"),
          sample("sample_jane_eyre", "Jane Eyre", "Charlotte Brontë"),
          sample("sample_war_worlds", "The War of the Worlds", "H. G. Wells"),
          sample("sample_alice", "Alice's Adventures in Wonderland", "Lewis Carroll"),
        ),
      ),
      itemCount = null,
    )

    private fun sample(
      id: String,
      title: String,
      author: String,
      progress: Float? = null,
    ): PreviewShelfItem {
      val base = libraryItem(
        id = id,
        userMediaProgress = progress?.let { mediaProgress(progress = it) },
      )
      val media = base.media as Media.Book
      return PreviewShelfItem(
        item = base.copy(
          media = media.copy(
            metadata = media.metadata.copy(
              title = title,
              subtitle = null,
              authorName = author,
              narratorName = SampleNarrator,
              seriesName = null,
            ),
          ),
        ),
      )
    }
  }
}

/**
 * One shelf of the Home scene. [label] is the server's name for it; null uses the preview's own
 * string, as the samples do.
 */
@Immutable
data class PreviewShelf(
  val label: String?,
  val items: List<PreviewShelfItem>,
)

/** A book or podcast on a shelf, with the [episode] the shelf points at for podcasts. */
@Immutable
data class PreviewShelfItem(
  val item: LibraryItem,
  val episode: PodcastEpisode? = null,
) {
  val key: String get() = item.id + episode?.id.orEmpty()
}

internal const val SampleNarrator = "Karen Savage"

internal const val SampleServerUrl = "https://abs.example.net"
