package app.campfire.podcasts.downloads

import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.podcasts.api.EpisodeDownloadsSnapshot

/**
 * Module-private (lives in `:features:podcasts:impl`) mutation surface for the server-side
 * podcast download tracker. The socket listener calls the per-event methods;
 * `DefaultPodcastsRepository` calls [applySnapshot] after hydrating via the REST endpoint.
 *
 * Module visibility — not the `internal` keyword — is what keeps this off the public API: only
 * code inside `:features:podcasts:impl` (which has KSP processing) can see it; consumers from
 * `:features:podcasts:ui` or `:app:common` only see the read-only
 * [app.campfire.podcasts.api.RemoteEpisodeDownloadTracker]. We can't mark it `internal` because
 * `DefaultPodcastsRepository` (public) and `PodcastEpisodeSocketListener` (public) inject it.
 */
interface RemoteEpisodeDownloadSink {
  fun onQueued(download: PodcastEpisodeDownload)
  fun onStarted(download: PodcastEpisodeDownload)
  fun onFinished(download: PodcastEpisodeDownload)
  fun onQueueCleared(libraryItemId: LibraryItemId)

  /**
   * Replace the in-memory state for all downloads in [libraryId] with [snapshot].
   * Entries belonging to other libraries are left alone.
   */
  fun applySnapshot(libraryId: LibraryId, snapshot: EpisodeDownloadsSnapshot)
}
