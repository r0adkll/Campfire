package app.campfire.audioplayer.impl.player

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.vlcj.ifPlayable
import app.campfire.audioplayer.impl.vlcj.isPlaying
import app.campfire.audioplayer.impl.vlcj.pause
import app.campfire.audioplayer.impl.vlcj.play
import app.campfire.audioplayer.impl.vlcj.seekBackward
import app.campfire.audioplayer.impl.vlcj.seekForward
import app.campfire.audioplayer.impl.vlcj.stop
import app.campfire.core.logging.bark
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.media.MediaRef
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.base.State
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent

/**
 * A wrapper around the vlcj sdk for playing audio via VLC. This class will take care of all communication
 * with the underlying player as well as handling media item parts, continuation, and playlists.
 *
 *
 */
class VlcPlayer {

  private val mediaItems = ArrayDeque<MediaItem>(20)
  private var currentItem = 0

  private var listener: Listener? = null

  private val mediaPlayer: MediaPlayer

  init {
    NativeDiscovery().discover()
    mediaPlayer = AudioPlayerComponent().mediaPlayer()
    mediaPlayer.events().addMediaPlayerEventListener(EventListener())
  }

  fun setListener(listener: Listener) {
    this.listener = listener
  }

  fun setMediaItems(items: List<MediaItem>) {
    if (mediaPlayer.isPlaying) {
      mediaPlayer.stop()
    }

    mediaItems.clear()
    mediaItems.addAll(items)
    currentItem = 0
  }

  fun setCurrentItem(index: Int) {
    if (index in mediaItems.indices) {
      currentItem = index
    }
  }

  fun prepare(playImmediately: Boolean, options: Array<out String>) {
    if (mediaItems.isNotEmpty() && currentItem in mediaItems.indices) {
      if (playImmediately) {
        startCurrentItem(*options)
      } else {
        prepareCurrentItem(*options)
      }
    }
  }

  fun pause() {
    mediaPlayer.pause()
  }

  fun playPause() {
    mediaPlayer.ifPlayable {
      if (isPlaying) {
        mediaPlayer.pause()
      } else {
        mediaPlayer.play()
      }
    }
  }

  fun stop() {
    mediaPlayer.stop()
  }

  fun seekTo(progress: Float) {
    mediaPlayer.controls().setPosition(progress)
  }

  fun seekTo(index: Int) {
    if (index in mediaItems.indices) {
      currentItem = index
      startCurrentItem()
    }
  }

  fun skipToNext() {
    if (currentItem < mediaItems.size - 1) {
      currentItem += 1
      startCurrentItem()
    }
  }

  fun skipToPrevious() {
    if (currentItem > 0) {
      currentItem -= 1
      startCurrentItem()
    }
  }

  fun seekForward(millis: Long) {
    mediaPlayer.seekForward(millis)
  }

  fun seekBackward(millis: Long) {
    mediaPlayer.seekBackward(millis)
  }

  fun setPlaybackSpeed(speed: Float) {
    mediaPlayer.controls().setRate(speed)
  }

  fun release() {
    mediaPlayer.release()
  }

  private fun startCurrentItem(vararg options: String) {
    mediaItems.getOrNull(currentItem)?.also { item ->
      val wasHandled = listener?.onMediaItemChanged(item) == true

      if (mediaPlayer.isPlaying) {
        mediaPlayer.stop()
      }

      val result = if (wasHandled) {
        mediaPlayer.media().startPaused(item.uri, *options)
      } else {
        mediaPlayer.media().play(item.uri, *options)
      }
      if (!result) {
        bark(TAG) { "Unable to start playback for ${item.uri}" }
      } else {
        bark(TAG) { "Starting ${item.uri}" }
      }
    }
  }

  private fun prepareCurrentItem(vararg options: String) {
    mediaItems.getOrNull(currentItem)?.also { item ->
      listener?.onMediaItemChanged(item)

      if (mediaPlayer.isPlaying) {
        mediaPlayer.stop()
      }

      val result = mediaPlayer.media().startPaused(item.uri, *options)
      if (!result) {
        bark(TAG) { "Unable to start playback for ${item.uri}" }
      } else {
        bark(TAG) { "Starting ${item.uri}" }
      }
    }
  }

  interface Listener {
    fun onStateChanged(state: AudioPlayer.State)
    fun onDurationChanged(durationInMillis: Long)
    fun onPositionChanged(positionInMillis: Long)
    fun onMediaItemChanged(mediaItem: MediaItem): Boolean
  }

  private inner class EventListener : MediaPlayerEventAdapter() {
    override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "mediaPlayerReady()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) {
      bark(TAG) { "mediaChanged($media)" }
      mediaPlayer?.syncStateToListener()
    }

    override fun opening(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "opening()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
      bark(TAG) { "buffering(newCache=$newCache)" }
      mediaPlayer?.syncStateToListener()
    }

    override fun playing(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "playing" }
      mediaPlayer?.syncStateToListener()
    }

    override fun paused(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "paused()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun stopped(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "stopped()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun forward(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "forward()" }
    }

    override fun backward(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "backward()" }
    }

    override fun finished(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "finished($mediaPlayer)" }
      listener?.onPositionChanged(0L)
      mediaPlayer?.submit {
        skipToNext()
      }
    }

    override fun seekableChanged(mediaPlayer: MediaPlayer?, newSeekable: Int) {
      bark(TAG) { "seekableChanged(newSeekable=$newSeekable)" }
    }

    override fun pausableChanged(mediaPlayer: MediaPlayer?, newPausable: Int) {
      bark(TAG) { "pausableChanged(newPausable=$newPausable)" }
    }

    override fun titleChanged(mediaPlayer: MediaPlayer?, newTitle: Int) {
      bark(TAG) { "titleChanged(newTitle=$newTitle)" }
    }

    override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
      bark(TAG) { "lengthChanged(newLength=$newLength)" }
      listener?.onDurationChanged(newLength)
    }

    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
      bark(TAG) { "timeChanged(newTime=$newTime)" }
      listener?.onPositionChanged(newTime)
      mediaPlayer?.syncStateToListener()
    }

    override fun corked(mediaPlayer: MediaPlayer?, corked: Boolean) {
      bark(TAG) { "corked(corked=$corked)" }
    }

    override fun muted(mediaPlayer: MediaPlayer?, muted: Boolean) {
      bark(TAG) { "muted(muted=$muted)" }
    }

    override fun audioDeviceChanged(mediaPlayer: MediaPlayer?, audioDevice: String?) {
      bark(TAG) { "audioDeviceChanged(audioDevice=$audioDevice)" }
    }

    override fun chapterChanged(mediaPlayer: MediaPlayer?, newChapter: Int) {
      bark(TAG) { "chapterChanged(newChapter=$newChapter)" }
    }

    override fun error(mediaPlayer: MediaPlayer?) {
      bark(TAG) { "error(${mediaPlayer?.status()?.state()})" }
      mediaPlayer?.syncStateToListener()
    }

    private fun MediaPlayer.syncStateToListener() {
      val state = status().state() ?: State.NOTHING_SPECIAL
      val playerState = when (state) {
        State.PLAYING -> AudioPlayer.State.Playing
        State.PAUSED -> AudioPlayer.State.Paused

        State.OPENING,
        State.BUFFERING,
        -> AudioPlayer.State.Buffering

        State.STOPPED,
        State.ERROR,
        State.ENDED,
        State.NOTHING_SPECIAL,
        -> AudioPlayer.State.Disabled
      }
      listener?.onStateChanged(playerState)
    }
  }
}

private const val TAG = "VlcPlayer"
