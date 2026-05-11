package app.campfire.podcasts.api

import androidx.paging.Pager
import app.campfire.core.model.User

interface PodcastsRepository {

  /**
   * Build a [Pager] over the user's currently selected podcast library's most recently published
   * episodes. The pager reads from the local SQLite cache as the source of truth and refreshes
   * via the server's `/api/libraries/:id/recent-episodes` endpoint. Episodes the user has marked
   * finished are excluded server-side.
   */
  fun createLatestEpisodesPager(user: User): Pager<Int, LatestEpisode>
}
