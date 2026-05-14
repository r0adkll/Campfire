package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * An episode parsed from a podcast's RSS feed that has not yet been ingested into the server library.
 * Returned by `GET /api/podcasts/:libraryItemId/search-episode` and accepted as the request body of
 * `POST /api/podcasts/:libraryItemId/download-episodes`.
 *
 * Distinct from [PodcastEpisode]: no server-assigned `id`, no `audioFile`/`audioTrack`, and many
 * fields may be empty strings rather than absent. The server is strict about field shapes when
 * downloading — round-trip the same object received from search.
 */
@Serializable
data class RssPodcastEpisode(
  val title: String,
  val subtitle: String = "",
  val description: String = "",
  val descriptionPlain: String = "",
  val pubDate: String = "",
  val episodeType: String = "",
  val season: String = "",
  val episode: String = "",
  val author: String = "",
  val duration: String = "",
  val durationSeconds: Int? = null,
  val explicit: String = "",
  val publishedAt: Long? = null,
  val enclosure: Enclosure,
  val guid: String? = null,
  val chaptersUrl: String? = null,
  val chaptersType: String? = null,
  val chapters: List<String> = emptyList(),
) {

  @Serializable
  data class Enclosure(
    val url: String,
    val type: String? = null,
    val length: String? = null,
  )
}
