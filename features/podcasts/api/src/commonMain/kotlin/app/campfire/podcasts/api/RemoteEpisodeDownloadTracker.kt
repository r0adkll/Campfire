package app.campfire.podcasts.api

import app.campfire.core.model.LibraryItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Read-only observer for the server's current podcast download activity.
 *
 * Holds in-memory state mirrored from the socket event stream
 * (`episode_download_queued` / `_started` / `_finished` / `_queue_cleared`).
 * Hydrated at startup and on demand via [refresh].
 *
 * Mutation lives behind a separate package-internal `RemoteEpisodeDownloadSink` inside
 * `:features:podcasts:impl` so consumers can't accidentally push state — only the socket
 * listener and the tracker impl wire that side.
 */
interface RemoteEpisodeDownloadTracker {
  /**
   * All known in-flight + queued downloads, grouped by their parent podcast's library item id.
   * Empty map means no downloads are happening anywhere.
   */
  val state: StateFlow<Map<LibraryItemId, List<RemoteEpisodeDownload>>>

  /**
   * Downloads scoped to a single podcast — convenience for detail / find-episodes screens.
   * Emits an empty list when nothing is queued or downloading for [libraryItemId].
   */
  fun observe(libraryItemId: LibraryItemId): Flow<List<RemoteEpisodeDownload>>
}
