package app.campfire.core.model

import kotlinx.datetime.LocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class Session(
  val libraryItem: LibraryItem,

  // Playback / Device Info
  val playMethod: PlayMethod,
  val mediaPlayer: String,

  // TODO: Not sure this is really needed?
//  val deviceInfo: DeviceInfo,

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

  val chapter: Chapter
    get() = libraryItem.getChapterForDuration(currentTime.inWholeMilliseconds)

  val title: String
    get() = chapter.title

  val chapterProgress: Float
    get() {
      val chapter = libraryItem.getChapterForDuration(currentTime.inWholeMilliseconds)
      val chapterCurrentTime = currentTime - chapter.start.toDouble().seconds
      val chapterDuration = (chapter.end - chapter.start).toDouble().seconds
      return chapterCurrentTime.inWholeMilliseconds.toFloat() /
        chapterDuration.inWholeMilliseconds.toFloat()
    }
}
