// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.flow.Flow

/**
 * Records and provides access to playback action history for library items.
 *
 * For podcasts, history is tracked per-episode: pass the episode's id to scope reads and
 * writes to a single episode. Pass `null` to target book/item-level history (or, in the
 * case of [observe]/[get]/[clear] without an episode, the entire item including all of
 * its episodes when no [episodeId] is provided).
 */
interface PlaybackHistoryRepository {

  /**
   * Observe playback actions for a given library item, ordered by most recent first.
   * If [episodeId] is provided, only that episode's actions are observed.
   */
  fun observe(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  ): Flow<List<PlaybackAction>>

  /**
   * Get playback actions for a given library item, ordered by most recent first.
   * If [episodeId] is provided, only that episode's actions are returned.
   */
  suspend fun get(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  ): List<PlaybackAction>

  /**
   * Delete playback history for a given library item. If [episodeId] is provided only
   * that episode's history is cleared; otherwise all history for the item is removed.
   */
  suspend fun clear(
    libraryItemId: LibraryItemId,
    episodeId: PodcastEpisodeId? = null,
  )

  /**
   * Delete all playback history for the current user.
   */
  suspend fun clearAll()
}
