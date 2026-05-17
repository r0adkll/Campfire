package app.campfire.network.models

import kotlinx.serialization.Serializable

/**
 * One row from `GET /api/search/podcast`. The server returns a bare JSON array of these objects
 * (no envelope). Fields mirror the iTunes Search API response the server proxies.
 *
 * Note: [id] and [artistId] arrive on the wire as numeric iTunes identifiers but the create
 * endpoint (`POST /api/podcasts`) requires `metadata.itunesId` / `metadata.itunesArtistId` as
 * strings — the mapper stringifies these at the boundary.
 */
@Serializable
data class PodcastSearchResultDto(
  val id: Long,
  val artistId: Long? = null,
  val title: String,
  val artistName: String? = null,
  val description: String? = null,
  val descriptionPlain: String? = null,
  val releaseDate: String? = null,
  val genres: List<String> = emptyList(),
  val cover: String? = null,
  val trackCount: Int? = null,
  val feedUrl: String? = null,
  val pageUrl: String? = null,
  val explicit: Boolean = false,
)
