// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId

/**
 * The item the current listening session is for: a book, or one episode of a podcast.
 *
 * It is the part of a session that "is this playing?" checks need, and it only changes when
 * something else starts playing, where the session itself changes with every saved position.
 */
data class PlayingItem(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
) {
  fun matches(libraryItemId: LibraryItemId, episodeId: PodcastEpisodeId? = null): Boolean =
    this.libraryItemId == libraryItemId && this.episodeId == episodeId
}
