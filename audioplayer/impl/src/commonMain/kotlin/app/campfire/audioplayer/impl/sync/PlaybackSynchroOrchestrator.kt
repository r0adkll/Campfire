package app.campfire.audioplayer.impl.sync

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.coroutines.DispatcherProvider
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import app.campfire.core.model.LibraryItemId
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@Inject
class PlaybackSynchroOrchestrator(
  private val audioPlayerHolder: AudioPlayerHolder,
  private val dispatcherProvider: DispatcherProvider,
  synchros: Set<PlaybackSynchronizer>,
) {

  private val sortedSynchronizers = synchros.sortedBy { it.rank }

  suspend fun sync() {
    audioPlayerHolder.currentPlayer
      .filterNotNull()
      .collectLatest { audioPlayer ->
        withContext(dispatcherProvider.computation) {
          val deferred = mutableListOf<Deferred<*>>()

          deferred += async {
            var lastState: AudioPlayer.State = AudioPlayer.State.Disabled
            audioPlayer.state
              .collect { s ->
                dispatchSynchronization(audioPlayer, "State") { onStateChanged(it, s, lastState) }
                lastState = s
              }
          }

          deferred += async {
            audioPlayer.overallTime
              .collect { time ->
                dispatchSynchronization(audioPlayer, "Overall Time") { onOverallTimeChanged(it, time) }
              }
          }

          deferred += async {
            audioPlayer.currentTime
              .collect { time ->
                dispatchSynchronization(audioPlayer, "Current Time") { onCurrentTimeChanged(it, time) }
              }
          }

          deferred += async {
            audioPlayer.currentDuration
              .collect { time ->
                dispatchSynchronization(audioPlayer, "Current Duration") {
                  onCurrentDurationChanged(it, time)
                }
              }
          }

          deferred += async {
            audioPlayer.currentMetadata
              .collect { meta ->
                dispatchSynchronization(audioPlayer, "Metadata") { onMetadataChanged(it, meta) }
              }
          }

          deferred += async {
            audioPlayer.playbackSpeed
              .collect { speed ->
                dispatchSynchronization(audioPlayer, "Playback Speed") { onPlaybackSpeedChanged(it, speed) }
              }
          }

          deferred.awaitAll()
          bark { "Synchronizer for $audioPlayer has finished" }
        }
      }
  }

  private suspend fun dispatchSynchronization(
    player: AudioPlayer,
    tag: String,
    block: suspend PlaybackSynchronizer.(LibraryItemId) -> Unit,
  ) {
    if (player.preparedSession == null) return
    sortedSynchronizers.forEach { synchro ->
      try {
        synchro.block(player.preparedSession!!.libraryItem.id)
      } catch (e: Exception) {
        if (e is CancellationException) throw e
        bark(
          priority = LogPriority.ERROR,
          throwable = e,
        ) { "Error synchronizing $tag for ${synchro::class.qualifiedName}" }
      }
    }
  }
}
