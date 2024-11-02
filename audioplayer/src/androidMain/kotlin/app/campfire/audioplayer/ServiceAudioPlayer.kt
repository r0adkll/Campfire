package app.campfire.audioplayer

import android.app.Application
import app.campfire.audioplayer.model.PlaybackSession
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.bark
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@SingleIn(AppScope::class)
@Inject
class ServiceAudioPlayer(
  private val application: Application,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AudioPlayer {

  private var delegateFlow: MutableStateFlow<AudioPlayer?> = MutableStateFlow(null)
  private val delegate: AudioPlayer?
    get() = delegateFlow.value

  fun setDelegate(player: AudioPlayer?) {
    delegateFlow.value = player
  }

  @OptIn(FlowPreview::class)
  override fun prepare(session: PlaybackSession, playWhenReady: Boolean, playbackRate: Float?) {
    if (delegate == null) {
      // Wait for the delegate to be set from the [AudioPlayerService] and then prepare the session, timing out
      // in 5 seconds.
      applicationScope.launch {
        delegateFlow
          .filterNotNull()
          .timeout(5.seconds)
          .catch {
            bark(tag = "ServiceAudioPlayer", throwable = it) { "Timeout waiting for AudioPlayerService to launch" }
          }
          .onEach { player ->
            player.prepare(session, playWhenReady, playbackRate)
          }
      }

      // Launch the audio player service on Android
      AudioPlayerService.start(application)
    } else {
      // Prepare the delegate if set
      delegate?.prepare(session, playWhenReady, playbackRate)
    }
  }

  override fun pause() {
    delegate?.pause()
  }

  override fun playPause() {
    delegate?.playPause()
  }

  override fun stop() {
    delegate?.stop()
  }

  override fun seekTo(positionInMs: Long) {
    delegate?.seekTo(positionInMs)
  }

  override fun skipToNext() {
    delegate?.skipToNext()
  }

  override fun skipToPrevious() {
    delegate?.skipToPrevious()
  }

  override fun seekForward(amount: Long?) {
    delegate?.seekForward(amount)
  }

  override fun seekBackward(amount: Long?) {
    delegate?.seekBackward(amount)
  }

  override fun setPlaybackSpeed(speed: Float) {
    delegate?.setPlaybackSpeed(speed)
  }
}
