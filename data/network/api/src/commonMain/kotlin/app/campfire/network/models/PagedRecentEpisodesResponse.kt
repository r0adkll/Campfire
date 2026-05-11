package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * Envelope for `GET /api/libraries/:id/recent-episodes`. The server does not return a `total`
 * or `nextPage`; callers should compute end-of-pagination as `episodes.size < limit`.
 */
@Serializable
data class PagedRecentEpisodesResponse(
  val episodes: List<RecentPodcastEpisode>,
  val limit: Int,
  val page: Int,
)
