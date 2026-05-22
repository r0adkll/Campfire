package app.campfire.podcasts.downloads

import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.podcasts.api.RemoteEpisodeDownload

/**
 * Convert a wire-level [PodcastEpisodeDownload] into the domain [RemoteEpisodeDownload], stamping
 * the lifecycle state externally. The wire model doesn't carry a discriminator — state is
 * implied by which bucket the download lives in (currentDownload vs queue) or which socket
 * event delivered it.
 */
fun PodcastEpisodeDownload.toDomain(state: RemoteEpisodeDownload.State): RemoteEpisodeDownload {
  return RemoteEpisodeDownload(
    id = id,
    libraryItemId = libraryItemId,
    libraryId = libraryId,
    url = url,
    episodeDisplayTitle = episodeDisplayTitle,
    podcastTitle = podcastTitle,
    state = state,
    createdAt = createdAt,
    startedAt = startedAt,
  )
}
