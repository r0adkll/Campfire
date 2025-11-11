package app.campfire.core.model.preview

import app.campfire.core.extensions.asSeconds
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.MediaType
import kotlin.time.Duration

fun mediaProgress(
  progress: Float = 0.64f,
  duration: Duration = previewLibraryItemDuration,
  isFinished: Boolean = false,
) = MediaProgress(
  id = "preview_media_progress_id",
  userId = "preview_user",
  libraryItemId = "preview_library_item",
  mediaItemId = "preview_media_item",
  duration = duration.asSeconds(),
  mediaItemType = MediaType.Book,
  progress = progress,
  currentTime = duration.times(progress.toDouble()).asSeconds(),
  hideFromContinueListening = false,
  lastUpdate = 0L,
  startedAt = 0L,
  isFinished = isFinished,
)
