package app.campfire.audioplayer.history

import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaybackActionType
import kotlin.time.Duration

interface PlaybackHistoryRecorder {

  fun record(
    libraryItemId: LibraryItemId,
    type: PlaybackActionType,
    fromPosition: Duration,
    toPosition: Duration? = null,
  )
}
