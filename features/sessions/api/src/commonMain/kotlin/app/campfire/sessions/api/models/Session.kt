package app.campfire.sessions.api.models

import app.campfire.core.model.LibraryItem
import kotlin.time.Duration
import kotlinx.datetime.LocalDateTime

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
