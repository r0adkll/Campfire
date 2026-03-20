package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

/**
 * Represents a discrete playback action performed by the user on a library item.
 *
 * For seek-type actions ([PlaybackActionType.Seek], [PlaybackActionType.SeekForward],
 * [PlaybackActionType.SeekBackward], [PlaybackActionType.SkipNext], [PlaybackActionType.SkipPrevious]),
 * [fromPosition] is the position before the action and [toPosition] is the resulting position.
 *
 * For non-seek actions, both fields hold the current playback position.
 */
data class PlaybackAction(
  val id: Long,
  val libraryItemId: LibraryItemId,
  val userId: String,
  val type: PlaybackActionType,
  val timestamp: LocalDateTime,
  val fromPosition: Duration,
  val toPosition: Duration,
)
