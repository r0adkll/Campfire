package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

/**
 * Represents a discrete playback action performed by the user on a library item.
 */
data class PlaybackAction(
  val id: Long,
  val libraryItemId: LibraryItemId,
  val userId: String,
  val type: PlaybackActionType,
  val timestamp: LocalDateTime,
  val fromPosition: Duration,
  val toPosition: Duration?,
)
