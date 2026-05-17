package app.campfire.network.envelopes

import app.campfire.network.models.PodcastFeed
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/podcasts/feed`. The server uses [rssFeed] to fetch and parse the
 * remote RSS document and returns the parsed feed in [PodcastFeedResponse].
 */
@Serializable
data class PodcastFeedRequest(
  val rssFeed: String,
)

/**
 * Response envelope for `POST /api/podcasts/feed`. The server wraps the parsed feed in a
 * `podcast` object that carries both podcast-level metadata and the episode list — see
 * [PodcastFeed].
 */
@Serializable
data class PodcastFeedResponse(
  val podcast: PodcastFeed,
)
