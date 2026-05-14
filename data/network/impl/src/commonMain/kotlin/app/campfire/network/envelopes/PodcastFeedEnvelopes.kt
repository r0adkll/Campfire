package app.campfire.network.envelopes

import app.campfire.network.models.RssPodcastEpisode
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/podcasts/feed`. The server uses [rssFeed] to fetch and parse the
 * remote RSS document and returns the resulting episode list in [PodcastFeedResponse].
 */
@Serializable
data class PodcastFeedRequest(
  val rssFeed: String,
)

/**
 * Response envelope for `POST /api/podcasts/feed`. The server wraps the parsed feed in a
 * `podcast` object containing the [episodes] list. The peer `metadata` field is intentionally
 * omitted: the feed-response metadata uses RSS-native shapes (e.g. `explicit` as `"yes"`/`"no"`)
 * that conflict with the strictly-typed [app.campfire.network.models.PodcastMetadata], and we
 * don't consume podcast-level metadata here.
 */
@Serializable
data class PodcastFeedResponse(
  val podcast: Podcast,
) {
  @Serializable
  data class Podcast(
    val episodes: List<RssPodcastEpisode> = emptyList(),
  )
}
