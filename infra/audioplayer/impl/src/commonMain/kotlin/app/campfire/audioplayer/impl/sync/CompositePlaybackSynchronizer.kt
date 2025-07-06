package app.campfire.audioplayer.impl.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.Metadata
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.model.LibraryItemId
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlin.time.Duration
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class CompositePlaybackSynchronizer(
  synchros: Set<PlaybackSynchronizer>,
) : PlaybackSynchronizer {

  private val sortedSynchros = synchros.sortedBy { it.rank }

  override suspend fun onStateChanged(
    sessionId: Uuid,
    libraryItemId: LibraryItemId,
    state: AudioPlayer.State,
    previousState: AudioPlayer.State,
  ) {
    sortedSynchros.forEach { s ->
      s.onStateChanged(sessionId, libraryItemId, state, previousState)
    }
  }

  override suspend fun onOverallTimeChanged(libraryItemId: LibraryItemId, overallTime: Duration) {
    sortedSynchros.forEach { s ->
      s.onOverallTimeChanged(libraryItemId, overallTime)
    }
  }

  override suspend fun onCurrentTimeChanged(libraryItemId: LibraryItemId, currentTime: Duration) {
    sortedSynchros.forEach { s ->
      s.onCurrentTimeChanged(libraryItemId, currentTime)
    }
  }

  override suspend fun onCurrentDurationChanged(libraryItemId: LibraryItemId, currentDuration: Duration) {
    sortedSynchros.forEach { s ->
      s.onCurrentDurationChanged(libraryItemId, currentDuration)
    }
  }

  override suspend fun onMetadataChanged(libraryItemId: LibraryItemId, metadata: Metadata) {
    sortedSynchros.forEach { s ->
      s.onMetadataChanged(libraryItemId, metadata)
    }
  }

  override suspend fun onPlaybackSpeedChanged(libraryItemId: LibraryItemId, playbackSpeed: Float) {
    sortedSynchros.forEach { s ->
      s.onPlaybackSpeedChanged(libraryItemId, playbackSpeed)
    }
  }
}
