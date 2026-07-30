// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.PodcastEpisodeId

/**
 * A single entry in the session queue. For books, [episode] is null. For podcast queue
 * entries, [episode] carries the playable episode and [libraryItem] is its parent podcast.
 */
data class QueuedEntry(
  val libraryItem: LibraryItem,
  val episode: PodcastEpisode? = null,
) {
  val libraryItemId: LibraryItemId
    get() = libraryItem.id

  val episodeId: PodcastEpisodeId?
    get() = episode?.id

  /**
   * Stable identity for queue rendering / reordering. Books use the bare library item id;
   * podcast entries combine library item id + episode id so multiple episodes of the same
   * podcast keep distinct keys.
   */
  val key: String
    get() = if (episode != null) "${libraryItem.id}:${episode.id}" else libraryItem.id
}
