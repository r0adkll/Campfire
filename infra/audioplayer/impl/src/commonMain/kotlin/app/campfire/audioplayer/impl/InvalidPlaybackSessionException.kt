package app.campfire.audioplayer.impl

import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Session
import app.campfire.core.model.loggableId
import kotlin.time.Duration.Companion.milliseconds

class InvalidPlaybackSessionException(
  session: Session,
  message: String,
) : Exception(
  """
    InvalidPlaybackSession(
      msg = $message,
      currentTime = ${session.currentTime},
      item = {
        id = ${session.libraryItem.id.loggableId},
        type = ${session.libraryItem.mediaType},
        numFiles = ${session.libraryItem.numFiles},
        media = {
          numTracks = ${session.libraryItem.media.numTracks},
          numAudioFiles = ${session.libraryItem.media.numAudioFiles},
          numChapters = ${session.libraryItem.media.numChapters},
          numInvalidAudioFiles = ${session.libraryItem.media.numInvalidAudioFiles},
          duration = ${session.libraryItem.media.durationInMillis.milliseconds},
          chapterRange = ${session.libraryItem.chapterRange()},
          trackRange = ${session.libraryItem.trackRange()},
        }
      }
    )
  """.trimIndent(),
)

private fun LibraryItem.chapterRange(): String {
  val firstChapter = media.chapters.firstOrNull()
  val lastChapter = media.chapters.lastOrNull()
  return "${firstChapter?.start?.seconds} --> ${lastChapter?.end?.seconds}"
}

private fun LibraryItem.trackRange(): String {
  val firstTrack = media.tracks.firstOrNull()
  val lastTrack = media.tracks.lastOrNull()
  return "${firstTrack?.startOffset?.seconds} --> ${(lastTrack?.let { it.startOffset + it.duration })?.seconds}"
}
