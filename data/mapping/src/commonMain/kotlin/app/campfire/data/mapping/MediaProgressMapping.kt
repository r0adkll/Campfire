package app.campfire.data.mapping

import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import app.campfire.data.MediaProgress as DatabaseMediaProgress
import app.campfire.network.models.MediaProgress as NetworkMediaProgress
import app.campfire.network.models.MediaType as NetworkMediaType

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

fun DatabaseMediaProgress.asDomainModel(): MediaProgress {
  return MediaProgress(
    id = id,
    userId = userId,
    libraryItemId = libraryItemId,
    episodeId = episodeId,
    mediaItemId = mediaItemId,
    mediaItemType = mediaItemType,
    duration = duration.toFloat(),
    progress = progress.toFloat(),
    currentTime = currentTime.toFloat(),
    isFinished = isFinished,
    hideFromContinueListening = hideFromContinueListening,
    ebookLocation = ebookLocation,
    ebookProgress = ebookProgress?.toFloat(),
    lastUpdate = lastUpdate,
    startedAt = startedAt,
  )
}
