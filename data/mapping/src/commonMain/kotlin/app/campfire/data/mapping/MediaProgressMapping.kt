package app.campfire.data.mapping

import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.data.MediaProgress as DatabaseMediaProgress
import app.campfire.network.models.MediaType as NetworkMediaType
import app.campfire.core.model.MediaType

fun NetworkMediaProgress.asDbModel(): DatabaseMediaProgress {
  return DatabaseMediaProgress(
    id = id,
    userId = userId,
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    mediaItemId = mediaItemId,
    mediaItemType = when (mediaItemType) {
      NetworkMediaType.Book -> MediaType.Book
      NetworkMediaType.Podcast -> MediaType.Podcast
    },
    duration = duration.toDouble(),
    progress = progress.toDouble(),
    currentTime = currentTime.toDouble(),
    isFinished = isFinished,
    hideFromContinueListening = hideFromContinueListening,
    ebookLocation = ebookLocation,
    ebookProgress = ebookProgress?.toDouble(),
    lastUpdate = lastUpdate,
    startedAt = startedAt,
    finishedAt = finishedAt,
  )
}
