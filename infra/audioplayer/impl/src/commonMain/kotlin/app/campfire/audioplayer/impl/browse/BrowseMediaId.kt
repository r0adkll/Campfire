package app.campfire.audioplayer.impl.browse

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PodcastEpisodeId

/**
 * Media3 browse/playback callbacks only carry a single string mediaId, so podcast episode
 * entries encode both the owning [LibraryItemId] and the [PodcastEpisodeId] into one id
 * that can be decoded back at playback time.
 */
internal data class BrowseMediaId(
  val libraryItemId: LibraryItemId,
  val episodeId: PodcastEpisodeId? = null,
) {

  fun encoded(): String = if (episodeId != null) {
    "$libraryItemId$EPISODE_SEPARATOR$episodeId"
  } else {
    libraryItemId
  }

  companion object {
    // Audiobookshelf ids are UUIDs or legacy `li_`/`ep_` style ids, neither of which
    // contain "::", so the separator can't collide with a real id.
    private const val EPISODE_SEPARATOR = "::"

    fun decode(mediaId: String): BrowseMediaId {
      val separatorIndex = mediaId.indexOf(EPISODE_SEPARATOR)
      if (separatorIndex == -1) return BrowseMediaId(mediaId)
      return BrowseMediaId(
        libraryItemId = mediaId.substring(0, separatorIndex),
        episodeId = mediaId.substring(separatorIndex + EPISODE_SEPARATOR.length)
          .takeIf { it.isNotEmpty() },
      )
    }
  }
}
