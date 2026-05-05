package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import app.campfire.core.model.PodcastEpisodeId
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

/**
 * Represents a discrete playback action performed by the user on a library item.
 *
 * For podcast episodes, [episodeId] identifies the specific episode whose timeline this
 * action belongs to. Null for book actions.
 */
data class PlaybackAction(
  val id: Long,
  val libraryItemId: LibraryItemId,
  val userId: String,
  val episodeId: PodcastEpisodeId? = null,
  val type: PlaybackActionType,
  val timestamp: LocalDateTime,
  val fromPosition: Duration,
  val toPosition: Duration?,
)
