package app.campfire.core.model

import app.campfire.core.extensions.progressOver
import app.campfire.core.extensions.seconds
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalUuidApi::class)
data class Session(
  val id: Uuid,
  val libraryItem: LibraryItem,
  val userId: UserId,
  val isDeleted: Boolean,

  // Playback / Device Info
  val playMethod: PlayMethod,
  val mediaPlayer: String,

  // Current Playback State
  val timeListening: Duration,
  val startTime: Duration,
  val currentTime: Duration,
  val lastPlayedAt: LocalDateTime?,

  // Date / Time
  val startedAt: LocalDateTime,
  val updatedAt: LocalDateTime,
) {
  val duration: Duration
    get() = libraryItem.media.durationInMillis.milliseconds

  val timeRemaining: Duration
    get() = duration - currentTime

  val progress: Float
    get() = currentTime.progressOver(duration)

  val isFinished: Boolean
    get() = currentTime >= duration

  val chapter: Chapter?
    get() = libraryItem.getChapterForDuration(currentTime.inWholeMilliseconds)

  val audioTrack: AudioTrack?
    get() = libraryItem.getAudioTrackForDuration(currentTime.inWholeMilliseconds)

  /**
   * Get the "Current" duration that should be displayed to the user. Starting
   * with the current chapter duration, the current track if chapter is not available,
   * and using the total duration if that fails.
   */
  val currentDuration: Duration
    get() = chapter?.duration ?: audioTrack?.duration?.seconds ?: duration

  val title: String
    get() = chapter?.title
      ?: audioTrack?.taggedTitle
      ?: libraryItem.media.metadata.title
      ?: TITLE_PLACEHOLDER

  companion object {
    const val TITLE_PLACEHOLDER = "--"
  }
}
