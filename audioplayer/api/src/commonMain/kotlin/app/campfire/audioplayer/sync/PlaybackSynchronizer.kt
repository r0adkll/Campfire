package app.campfire.audioplayer.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.core.model.Session
import kotlin.time.Duration

interface PlaybackSynchronizer {
  val rank: Int get() = RANK_DEFAULT

  suspend fun onStateChanged(session: Session, state: AudioPlayer.State) = Unit
  suspend fun onOverallTimeChanged(session: Session, overallTime: Duration) = Unit
  suspend fun onCurrentTimeChanged(session: Session, currentTime: Duration) = Unit
  suspend fun onCurrentDurationChanged(session: Session, currentDuration: Duration) = Unit
  suspend fun onMetadataChanged(session: Session, metadata: Metadata) = Unit
  suspend fun onPlaybackSpeedChanged(session: Session, playbackSpeed: Float) = Unit

  companion object {
    const val RANK_LOWEST = Int.MIN_VALUE
    const val RANK_DEFAULT = 0
    const val RANK_HIGHEST = Int.MAX_VALUE
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
