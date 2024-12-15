package app.campfire.core.model

import app.campfire.core.extensions.progressOver
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalUuidApi::class)
data class Session(
  val id: Uuid,
  val libraryItem: LibraryItem,

  // Playback / Device Info
  val playMethod: PlayMethod,
  val mediaPlayer: String,

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

  val progress: Float
    get() = currentTime.progressOver(duration)

  val isFinished: Boolean
    get() = currentTime >= duration

  val chapter: Chapter
    get() = libraryItem.getChapterForDuration(currentTime.inWholeMilliseconds)

  val title: String
    get() = chapter.title
}
