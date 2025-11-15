package app.campfire.data.mapping

import app.campfire.core.model.Chapter
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

fun Chapter.asDbModel(mediaId: String): MediaChapters {
  return MediaChapters(
    mediaId = mediaId,
    id = id,
    start = start.toDouble(),
    end = end.toDouble(),
    title = title,
  )
}

fun BookChapter.asDomainModel(): Chapter {
  return Chapter(
    id = id,
    start = start,
    end = end,
    title = title,
  )
}
