// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.api

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisodeId
import kotlinx.coroutines.flow.Flow

/**
 * Predicts how a *streamed* (non-downloaded) playback of an item would be delivered, per the
 * user's streaming-method setting and the large-item heuristic. This is the single source of
 * truth for the HLS-vs-direct decision: the session router consults it when creating a
 * session, and UI surfaces consult it to reflect the same outcome ahead of playback.
 *
 * Downloads are out of scope — they always play locally, and callers gate on download state
 * themselves.
 */
interface StreamingRoutePredictor {

  /** Whether or not we COULD stream hls with our settings. */
  fun canStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId? = null): Boolean

  /** The decision as of the current settings. */
  fun wouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId? = null): Boolean

  /** The decision, re-emitted as the underlying settings change. */
  fun observeWouldStreamHls(libraryItem: LibraryItem, episodeId: PodcastEpisodeId? = null): Flow<Boolean>
}
