package app.campfire.common.test

import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PlayMethod
import app.campfire.core.model.Session
import app.campfire.core.model.UserId
import app.campfire.home.ui.libraryItem
import kotlin.time.Duration
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

fun session(
  id: Uuid = Uuid.random(),
  libraryItem: LibraryItem = libraryItem(),
  userId: UserId = "user_id",
  playMethod: PlayMethod = PlayMethod.DirectPlay,
  mediaPlayer: String = "Campfire",
  timeListening: Duration = Duration.ZERO,
  startTime: Duration = Duration.ZERO,
  currentTime: Duration = Duration.ZERO,
  startedAt: LocalDateTime = LocalDateTime(2025, 1, 1, 1, 1),
  updatedAt: LocalDateTime = LocalDateTime(2025, 1, 1, 1, 1),
) = Session(
  id = id,
  libraryItem = libraryItem,
  userId = userId,
  playMethod = playMethod,
  mediaPlayer = mediaPlayer,
  timeListening = timeListening,
  startTime = startTime,
  currentTime = currentTime,
  startedAt = startedAt,
  updatedAt = updatedAt,
)
