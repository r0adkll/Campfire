package app.campfire.audioplayer.impl.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.logging.Cork
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlin.time.Duration
import kotlin.uuid.Uuid

@ContributesMultibinding(AppScope::class, boundType = PlaybackSynchronizer::class)
object LoggingSynchronizer : PlaybackSynchronizer, Cork {

  override val rank: Int = PlaybackSynchronizer.RANK_HIGHEST
  override val tag: String = "LoggingSynchronizer"

  // This Logger is VERY verbose and should only be deliberately
  // turned on for debugging and off by default
  override val enabled: Boolean = false

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    dbark { "onStateChanged(sessionId=$sessionId, id=$libraryItemId, state=$state, previousState=$previousState)" }
  }

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    dbark { "onOverallTimeChanged(id=$libraryItemId, overallTime=$overallTime)" }
  }

  override suspend fun onCurrentTimeChanged(libraryItemId: LibraryItemId, currentTime: Duration) {
    dbark { "onCurrentTimeChanged(id=$libraryItemId, currentTime=$currentTime)" }
  }

  override suspend fun onCurrentDurationChanged(libraryItemId: LibraryItemId, currentDuration: Duration) {
    dbark { "onCurrentDurationChanged(id=$libraryItemId, currentDuration=$currentDuration)" }
  }

  override suspend fun onMetadataChanged(libraryItemId: LibraryItemId, metadata: Metadata) {
    dbark { "onMetadataChanged(id=$libraryItemId, metadata=$metadata)" }
  }

  override suspend fun onPlaybackSpeedChanged(libraryItemId: LibraryItemId, playbackSpeed: Float) {
    dbark { "onPlaybackSpeedChanged(id=$libraryItemId, playbackSpeed=$playbackSpeed)" }
  }
}
