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
          chapters = [
            ${session.libraryItem.chaptersAsDebugString()}
          ],
          tracks = [
            ${session.libraryItem.tracksAsDebugString()}
          ],
        }
      }
    )
  """.trimIndent(),
)

private fun LibraryItem.chaptersAsDebugString(): String {
  return media.chapters.joinToString(",\n") {
    "[${it.id}] ${it.start.seconds} -> ${it.end.seconds}"
  }
}

private fun LibraryItem.tracksAsDebugString(): String {
  return media.tracks.joinToString(",\n") {
    "[${it.index}] ${it.startOffset.seconds} -> ${(it.startOffset + it.duration).seconds}"
  }
}
