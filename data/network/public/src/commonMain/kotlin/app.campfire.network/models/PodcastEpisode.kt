package app.campfire.network.models

import kotlinx.serialization.SerialName
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

data class PodcastEpisode (

  /* The ID of library items after 2.3.0. */
    @SerialName(value = "libraryItemId")
    val libraryItemId: String,

  /* The ID of podcasts and podcast episodes after 2.3.0. */
    @SerialName(value = "podcastId")
    val podcastId: String,

  /* The ID of podcasts and podcast episodes after 2.3.0. */
    @SerialName(value = "id")
    val id: String,

  /* The ID of podcasts on server version 2.2.23 and before. */
    @SerialName(value = "oldEpisodeId")
    val oldEpisodeId: String? = null,

  /* The index of the episode within the podcast. */
    @SerialName(value = "index")
    val index: Int? = null,

  /* The season number of the episode. */
    @SerialName(value = "season")
    val season: String? = null,

  /* The episode number within the season. */
    @SerialName(value = "episode")
    val episode: String? = null,

  /* The type of episode (e.g., full, trailer). */
    @SerialName(value = "episodeType")
    val episodeType: String? = null,

  /* The title of the episode. */
    @SerialName(value = "title")
    val title: String? = null,

  /* The subtitle of the episode. */
    @SerialName(value = "subtitle")
    val subtitle: String? = null,

  /* The description of the episode. */
    @SerialName(value = "description")
    val description: String? = null,

  /* The enclosure object containing additional episode data. */
    @SerialName(value = "enclosure")
    val enclosure: Map<String, String>? = null,

  /* The globally unique identifier for the episode. */
    @SerialName(value = "guid")
    val guid: String? = null,

  /* The publication date of the episode. */
    @SerialName(value = "pubDate")
    val pubDate: String? = null,

  /* The chapters within the episode. */
    @SerialName(value = "chapters")
    val chapters: List<String>? = null,

  @SerialName(value = "audioFile")
    val audioFile: AudioFile? = null,

  /* The time (in ms since POSIX epoch) when was created. */
    @SerialName(value = "publishedAt")
    val publishedAt: Int? = null,

  /* The time (in ms since POSIX epoch) when added to the server. */
    @SerialName(value = "addedAt")
    val addedAt: Int? = null,

  /* The time (in ms since POSIX epoch) when last updated. */
    @SerialName(value = "updatedAt")
    val updatedAt: Int? = null,

  @SerialName(value = "audioTrack")
    val audioTrack: AudioTrack? = null,

  /* The total length (in seconds) of the item or file. */
    @SerialName(value = "duration")
    val duration: Double,

  /* The total size (in bytes) of the item or file. */
    @SerialName(value = "size")
    val propertySize: Int? = null

) {


}

