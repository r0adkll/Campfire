package app.campfire.podcasts.api

import androidx.paging.Pager
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.User

interface PodcastsRepository {

  /**
   * Build a [Pager] over the user's currently selected podcast library's most recently published
   * episodes. The pager reads from the local SQLite cache as the source of truth and refreshes
   * via the server's `/api/libraries/:id/recent-episodes` endpoint. Episodes the user has marked
   * finished are excluded server-side.
   */
  fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode>

  /**
   * Fetch the parsed RSS feed at [rssFeedUrl] and return all episodes the server-side parser
   * extracted. Sort order matches the source feed (typically newest first); callers should still
   * sort defensively on [RemotePodcastEpisode.publishedAtMillis]. Requires an Admin or Root
   * account — non-admin callers receive a failure wrapping the server's 403 response.
   */
  suspend fun fetchPodcastFeed(rssFeedUrl: String): Result<List<RemotePodcastEpisode>>

  /**
   * Queue the given RSS-derived [episodes] for download into the podcast identified by
   * [libraryItemId]. Requires an Admin or Root account; non-admin callers receive a failure that
   * wraps the server's 403 response. The server acks immediately and downloads asynchronously.
   */
  suspend fun queueEpisodeDownloads(
    libraryItemId: LibraryItemId,
    episodes: List<RemotePodcastEpisode>,
  ): Result<Unit>
}
