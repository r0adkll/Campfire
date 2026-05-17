package app.campfire.network.models
import kotlinx.serialization.Serializable

/**
 * A single episode of a podcast.
 *
 * @param libraryItemId The ID of library items after 2.3.0.
 * @param podcastId The ID of podcasts and podcast episodes after 2.3.0.
 * @param id The ID of podcasts and podcast episodes after 2.3.0.
 * @param oldEpisodeId The ID of podcasts on server version 2.2.23 and before.
 * @param index The index of the episode within the podcast.
 * @param season The season number of the episode.
 * @param episode The episode number within the season.
 * @param episodeType The type of episode (e.g., full, trailer).
 * @param title The title of the episode.
 * @param subtitle The subtitle of the episode.
 * @param description The description of the episode.
 * @param enclosure The enclosure object containing additional episode data.
 * @param guid The globally unique identifier for the episode.
 * @param pubDate The publication date of the episode.
 * @param chapters The chapters within the episode.
 * @param audioFile
 * @param publishedAt The time (in ms since POSIX epoch) when was created.
 * @param addedAt The time (in ms since POSIX epoch) when added to the server.
 * @param updatedAt The time (in ms since POSIX epoch) when last updated.
 * @param audioTrack
 * @param duration The total length (in seconds) of the item or file.
 * @param propertySize The total size (in bytes) of the item or file.
 */
@Serializable
data class PodcastEpisode(
  val libraryItemId: String,
  val podcastId: String,
  val id: String,
  val oldEpisodeId: String? = null,
  val index: Int? = null,
  val season: String? = null,
  val episode: String? = null,
  val episodeType: String? = null,
  val title: String,
  val subtitle: String? = null,
  val description: String? = null,
  val enclosure: Enclosure? = null,
  val guid: String? = null,
  val pubDate: String? = null,
  val chapters: List<Chapter> = emptyList(),
  val audioFile: AudioFile,
  val publishedAt: Long? = null,
  val addedAt: Long,
  val updatedAt: Long,

  // Expanded
  val audioTrack: AudioTrack? = null,
  val duration: Double? = null,
  val propertySize: Int? = null,
) {

  @Serializable
  data class Chapter(
    val start: Double,
    val end: Double,
    val title: String,
    val id: Int,
  )

  @Serializable
  data class Enclosure(
    val url: String,
    val type: String? = null,
    val length: Long? = null,
  )
}
