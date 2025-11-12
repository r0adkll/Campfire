package app.campfire.audioplayer.test

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAudioPlayerHolder : AudioPlayerHolder {

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  override fun setCurrentPlayer(player: AudioPlayer?) {
    currentPlayer.value = player
  }

  override fun release() {
    currentPlayer.value?.release()
    currentPlayer.value = null
  }
}
