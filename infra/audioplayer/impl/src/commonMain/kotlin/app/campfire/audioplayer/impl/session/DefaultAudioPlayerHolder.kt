package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.audioplayer.sync.PlaybackSynchronizer
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultAudioPlayerHolder(
  @ForScope(AppScope::class) private val scope: CoroutineScope,
  private val synchronizer: PlaybackSynchronizer,
) : AudioPlayerHolder {

  private var synchronizerJobs: List<Job> = emptyList()

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  override fun setCurrentPlayer(player: AudioPlayer?) {
    currentPlayer.value = player
    cancelSynchronizer()
    player?.let { registerSynchronizer(it) }
  }

  override fun release() {
    currentPlayer.value?.release()
    currentPlayer.value = null
    cancelSynchronizer()
  }

  private fun cancelSynchronizer() {
    synchronizerJobs.forEach { it.cancel() }
  }

  private fun registerSynchronizer(player: AudioPlayer) {
    synchronizerJobs += player.state
      .scan(player.state.value) { lastState, state ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@scan state
        synchronizer.onStateChanged(libraryItemId, state, lastState)
        state
      }
      .launchIn(scope)

    synchronizerJobs += player.currentTime
      .onEach { currentTime ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@onEach
        synchronizer.onCurrentTimeChanged(libraryItemId, currentTime)
      }
      .launchIn(scope)

    synchronizerJobs += player.currentDuration
      .onEach { currentDuration ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@onEach
        synchronizer.onCurrentDurationChanged(libraryItemId, currentDuration)
      }
      .launchIn(scope)

    synchronizerJobs += player.overallTime
      .onEach { overallTime ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@onEach
        synchronizer.onOverallTimeChanged(libraryItemId, overallTime)
      }
      .launchIn(scope)

    synchronizerJobs += player.currentMetadata
      .onEach { metadata ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@onEach
        synchronizer.onMetadataChanged(libraryItemId, metadata)
      }
      .launchIn(scope)

    synchronizerJobs += player.playbackSpeed
      .onEach { speed ->
        val libraryItemId = player.preparedSession?.libraryItem?.id ?: return@onEach
        synchronizer.onPlaybackSpeedChanged(libraryItemId, speed)
      }
      .launchIn(scope)
  }
}
