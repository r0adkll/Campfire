package app.campfire.audioplayer.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.Session
import kotlin.time.Duration

interface PlaybackSynchronizer {
  val rank: Int get() = RANK_DEFAULT

  suspend fun onStateChanged(
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) = Unit
  suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) = Unit
  suspend fun onCurrentTimeChanged(libraryItemId: LibraryItemId, currentTime: Duration) = Unit
  suspend fun onCurrentDurationChanged(libraryItemId: LibraryItemId, currentDuration: Duration) = Unit
  suspend fun onMetadataChanged(libraryItemId: LibraryItemId, metadata: Metadata) = Unit
  suspend fun onPlaybackSpeedChanged(libraryItemId: LibraryItemId, playbackSpeed: Float) = Unit

  companion object {
    const val RANK_LOWEST = Int.MAX_VALUE
    const val RANK_DEFAULT = 0
    const val RANK_HIGHEST = Int.MIN_VALUE
  }
}

data class AudioPlayerSyncState(
  val session: Session?,
  val state: AudioPlayer.State,
  val overallTime: Duration,
  val currentTime: Duration,
  val currentDuration: Duration,
  val metadata: Metadata,
  val playbackSpeed: Float,
)
