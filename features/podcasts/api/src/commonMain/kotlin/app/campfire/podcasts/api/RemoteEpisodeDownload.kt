package app.campfire.podcasts.api

import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId

/**
 * A snapshot of one in-flight or queued podcast episode download from the Audiobookshelf server.
 *
 * Lives in client memory only — populated from the `GET /api/libraries/{id}/episode-downloads`
 * endpoint at startup and kept in sync via socket events (`episode_download_queued`,
 * `episode_download_started`, `episode_download_finished`, `episode_download_queue_cleared`).
 *
 * Once a download finishes (success or failure), the server drops it from its in-memory state
 * and we drop it from ours — there is no "history" of past downloads.
 */
data class RemoteEpisodeDownload(
  /** Server-assigned id for this download. Unique across all downloads on the server. */
  val id: String,

  /** The parent podcast (library item) this episode belongs to. */
  val libraryItemId: LibraryItemId,

  /** The library the parent podcast lives in. */
  val libraryId: LibraryId?,

  /**
   * The RSS enclosure URL the server is downloading from. The server uses this as the dedupe
   * key — duplicate downloads for the same URL are silently ignored — so this is also the key
   * we match against `RemotePodcastEpisode.enclosureUrl` when correlating feed rows to live
   * download state.
   */
  val url: String,

  /** Human-readable episode title for display; may be null if RSS metadata is sparse. */
  val episodeDisplayTitle: String?,

  /** Human-readable podcast title for display; may be null if RSS metadata is sparse. */
  val podcastTitle: String?,

  /** Current lifecycle state. */
  val state: State,

  /** Epoch ms when the server queued the download. */
  val createdAt: Long?,

  /** Epoch ms when the server started downloading. `null` while [state] is [State.Queued]. */
  val startedAt: Long?,
) {
  enum class State {
    /** In the server's queue, not yet downloading. */
    Queued,

    /** Actively downloading on the server. */
    Downloading,
  }
}
