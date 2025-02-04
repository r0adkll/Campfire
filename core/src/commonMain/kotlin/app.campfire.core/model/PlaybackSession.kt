package app.campfire.core.model

import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

/**
 * Pure representation of a playback session from the server
 */
data class PlaybackSession(
  val id: String,
  val userId: String,
  val libraryId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaType: String,
  val mediaMetadata: Media.Metadata,
  val chapters: List<Chapter>,
  val displayTitle: String,
  val displayAuthor: String,
  val coverImageUrl: String,
  val duration: Duration,
  val playMethod: Int,
  val mediaPlayer: String,
  val deviceInfo: DeviceInfo,
  val serverVersion: String,
  val date: String,
  val dayOfWeek: String,
  val timeListening: Float,
  val startTime: Float,
  val currentTime: Float,
  val startedAt: LocalDateTime,
  val updatedAt: LocalDateTime,
)
