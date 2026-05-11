package app.campfire.network.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The flat episode payload returned by `GET /api/libraries/:id/recent-episodes`. Mirrors the
 * standard episode shape (`toOldJSONExpanded` server-side) but adds the parent [podcast] context
 * and [libraryId] at the top level, so the client can render this feed without a follow-up fetch.
 *
 * Most fields are defaulted/nullable to keep deserialization tolerant of the server occasionally
 * omitting empty strings, missing audio data on broken episodes, etc.
 */
@Serializable
data class RecentPodcastEpisode(
  val libraryItemId: String,
  val podcastId: String,
  val id: String,
  val oldEpisodeId: String? = null,
  val index: Int? = null,
  val season: String = "",
  val episode: String = "",
  val episodeType: String = "full",
  val title: String,
  val subtitle: String = "",
  val description: String = "",
  val enclosure: PodcastEpisode.Enclosure? = null,
  val guid: String? = null,
  val pubDate: String = "",
  val chapters: List<String> = emptyList(),
  val audioFile: AudioFile? = null,
  val publishedAt: Long? = null,
  val addedAt: Long,
  val updatedAt: Long,
  val audioTrack: AudioTrack? = null,
  val duration: Double = 0.0,
  @SerialName("size")
  val sizeBytes: Long = 0L,
  val podcast: Podcast,
  val libraryId: String,
)
