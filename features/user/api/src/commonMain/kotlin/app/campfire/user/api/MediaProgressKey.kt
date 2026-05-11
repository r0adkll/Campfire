package app.campfire.user.api

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.PodcastEpisodeId

/**
 * Shared class for keying media progress against both books and podcast episodes
 */
data class MediaProgressKey(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
) {
  constructor(progress: MediaProgress) : this(
    libraryItemId = progress.libraryItemId,
    episodeId = progress.episodeId,
  )
}
