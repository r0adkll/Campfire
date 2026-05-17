package app.campfire.network.envelopes

import app.campfire.network.models.PodcastMetadata
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/podcasts`. Creates a new podcast library item from a feed URL —
 * the server validates that [path] is a sub-path of the selected folder and rejects duplicates.
 * Requires Admin or Root.
 */
@Serializable
data class CreatePodcastRequest(
  val libraryId: String,
  val folderId: String,
  val path: String,
  val media: CreatePodcastMedia,
)

/**
 * The `media` object inside [CreatePodcastRequest]. [autoDownloadSchedule] accepts a cron
 * expression; if null the server falls back to the global `ServerSettings.podcastEpisodeSchedule`.
 * `maxEpisodesToKeep` and `maxNewEpisodesToDownload` are set to server defaults and not accepted
 * from the request body.
 */
@Serializable
data class CreatePodcastMedia(
  val metadata: PodcastMetadata,
  val tags: List<String> = emptyList(),
  val autoDownloadEpisodes: Boolean = false,
  val autoDownloadSchedule: String? = null,
)
