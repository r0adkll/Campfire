package app.campfire.audioplayer

import kotlinx.coroutines.flow.StateFlow

/**
 * An interface that exists in the [app.campfire.core.di.AppScope] to
 * provide access to the current per-platform-instance for playback use.
 */
interface AudioPlayerHolder {
  val currentPlayer: StateFlow<AudioPlayer?>

  fun setCurrentPlayer(player: AudioPlayer?)
  fun release()
}
