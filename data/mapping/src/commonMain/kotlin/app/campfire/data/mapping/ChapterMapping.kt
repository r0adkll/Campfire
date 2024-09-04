package app.campfire.data.mapping

import app.campfire.data.MediaChapters
import app.campfire.network.models.BookChapter

fun BookChapter.asDbModel(mediaId: String): MediaChapters {
  return MediaChapters(
    mediaId = mediaId,
    id = id,
    start = start.toDouble(),
    end = end.toDouble(),
    title = title,
  )
}
