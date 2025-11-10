package app.campfire.common.test

import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaProgressId
import app.campfire.core.model.MediaType
import kotlin.random.Random

fun mediaProgress(
  libraryItemId: String,
  id: MediaProgressId = "some_${Random.nextInt()}",
  userId: String = "some_user",
  episodeId: String? = null,
  mediaItemId: String = "some_media_id",
  mediaItemType: MediaType = MediaType.Book,
  duration: Float = 100f,
  progress: Float = 0f,
  currentTime: Float = 0f,
  isFinished: Boolean = false,
  hideFromContinueListening: Boolean = false,
  ebookLocation: String? = null,
  ebookProgress: Float? = null,
  lastUpdate: Long = 0L,
  startedAt: Long = 0L,
  finishedAt: Long? = null,
): MediaProgress = MediaProgress(
  id = id,
  userId = userId,
  libraryItemId = libraryItemId,
  episodeId = episodeId,
  mediaItemId = mediaItemId,
  mediaItemType = mediaItemType,
  duration = duration,
  progress = progress,
  currentTime = currentTime,
  isFinished = isFinished,
  hideFromContinueListening = hideFromContinueListening,
  ebookLocation = ebookLocation,
  ebookProgress = ebookProgress,
  lastUpdate = lastUpdate,
  startedAt = startedAt,
  finishedAt = finishedAt,
)
