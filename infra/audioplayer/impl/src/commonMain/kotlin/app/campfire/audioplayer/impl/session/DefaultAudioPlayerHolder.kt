package app.campfire.audioplayer.impl.session

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DefaultAudioPlayerHolder : AudioPlayerHolder {

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  override fun setCurrentPlayer(player: AudioPlayer?) {
    currentPlayer.value = player
  }

  override fun release() {
    currentPlayer.value?.release()
    currentPlayer.value = null
  }
}
