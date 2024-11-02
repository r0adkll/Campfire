package app.campfire.core.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration

typealias SessionId = String

data class Session(
  val id: SessionId,
  val libraryItem: LibraryItem,

  // Playback / Device Info
  val playMethod: PlayMethod,
  val mediaPlayer: String,
  val deviceInfo: DeviceInfo,

  // Current Playback State
  val duration: Duration,
  val timeListening: Duration,
  val startTime: Duration,
  val currentTime: Duration,

  // Date / Time
  val startedAt: LocalDateTime,
  val updatedAt: LocalDateTime,
) {

  val timeRemaining: Duration
    get() = duration - currentTime
}
