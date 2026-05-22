package app.campfire.podcasts.api

import androidx.paging.Pager
import app.campfire.core.model.LibraryId
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User
import app.campfire.libraries.api.LibraryFolder

interface PodcastsRepository {

  /**
   * Build a [Pager] over the user's currently selected podcast library's most recently published
   * episodes. The pager reads from the local SQLite cache as the source of truth and refreshes
   * via the server's `/api/libraries/:id/recent-episodes` endpoint. Episodes the user has marked
   * finished are excluded server-side.
   */
  fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode>

  /**
   * Fetch and parse the RSS feed at [rssFeedUrl], returning both the podcast-level metadata
   * (as a [PodcastDraft]) and the full episode list. Callers consume one or both facets:
   * "Find episodes" uses only `.episodes`, the "Add podcast" builder uses both. Requires an
   * Admin or Root account — non-admin callers receive a failure wrapping the server's 403.
   */
  suspend fun fetchPodcastFeedDetails(rssFeedUrl: String): Result<PodcastFeedDetails>

  /**
   * Queue the given RSS-derived [episodes] for download into the podcast identified by
   * [libraryItemId]. Requires an Admin or Root account; non-admin callers receive a failure that
   * wraps the server's 403 response. The server acks immediately and downloads asynchronously.
   */
  suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit>

  /**
   * Fetch the current server-side podcast download queue for [libraryId] — the actively
   * downloading episode (if any) and the queued items. Used to hydrate
   * [RemoteEpisodeDownloadTracker] at startup and on pull-to-refresh.
   */
  suspend fun fetchEpisodeDownloads(libraryId: LibraryId): Result<EpisodeDownloadsSnapshot>

  /**
   * Clear all queued downloads for the podcast identified by [libraryItemId]. Does NOT
   * interrupt the actively-downloading item — the server has no API for that. Requires an
   * Admin or Root account; non-admin callers receive a failure wrapping the server's 403.
   * The server emits `episode_download_queue_cleared` on success.
   */
  suspend fun clearEpisodeDownloadQueue(libraryItemId: LibraryItemId): Result<Unit>

  /**
   * Search the server's podcast directory (iTunes proxy) for podcasts matching [query]. Pass
   * [country] to override the server's default region. Results without a feed URL are filtered
   * out — only candidates that can be added as podcast items survive. Available to any
   * authenticated user.
   */
  suspend fun searchPodcasts(
    query: String,
    country: String? = null,
  ): Result<List<PodcastSearchResult>>

  /**
   * Create a new podcast library item in [libraryId] under [folder] from [draft]. The path is
   * computed as `${folder.fullPath}/${sanitized title}` — the server rejects collisions with HTTP
   * 400 (surfaced as a failure). Requires an Admin or Root account. Returns the new library
   * item's id on success.
   */
  suspend fun addPodcast(
    libraryId: LibraryId,
    folder: LibraryFolder,
    draft: PodcastDraft,
    autoDownloadEpisodes: Boolean,
  ): Result<LibraryItemId>
}
