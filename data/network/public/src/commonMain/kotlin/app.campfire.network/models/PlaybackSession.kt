package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackSession(
  val id: String,
  val userId: String,
  val libraryId: String,
  val libraryItemId: String,
  val episodeId: String? = null,
  val mediaType: String,
  val mediaMetadata: MinifiedBookMetadata,
  val chapters: List<BookChapter>,
  val displayTitle: String,
  val displayAuthor: String,
  val coverPath: String?,
  val duration: Double,
  val playMethod: Int,
  val mediaPlayer: String,
  val deviceInfo: DeviceInfo,
  val serverVersion: String,
  val date: String,
  val dayOfWeek: String,
  val timeListening: Float,
  val startTime: Float,
  val currentTime: Float,
  val startedAt: Long,
  val updatedAt: Long,
)
