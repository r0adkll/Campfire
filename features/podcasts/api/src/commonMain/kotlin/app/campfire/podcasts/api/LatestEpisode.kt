package app.campfire.podcasts.api

import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisode

/**
 * One row of the cross-podcast Latest Episodes feed. Pairs a [PodcastEpisode] with the parent
 * podcast's presentation context (cover, title, author) since each row in this feed comes from
 * a different show, and with the user's progress on this episode if any.
 */
data class LatestEpisode(
  val episode: PodcastEpisode,
  val podcastTitle: String?,
  val podcastAuthor: String?,
  val coverImageUrl: String?,
  val progress: MediaProgress?,
)
