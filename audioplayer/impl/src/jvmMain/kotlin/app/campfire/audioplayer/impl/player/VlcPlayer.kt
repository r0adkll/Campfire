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
import kotlin.math.roundToLong
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

  var currentItemIndex = 0
    private set

  val currentMediaItem: MediaItem
    get() = mediaItems[currentItemIndex]

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
    currentItemIndex = 0
  }

  fun getMediaItemAt(index: Int): MediaItem {
    return mediaItems[index]
  }

  fun setCurrentItem(index: Int) {
    if (index in mediaItems.indices) {
      currentItemIndex = index
    }
  }

  fun prepare(playImmediately: Boolean, options: Array<out VlcOption>) {
    if (mediaItems.isNotEmpty() && currentItemIndex in mediaItems.indices) {
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
    currentMediaItem.clipping?.let { clipping ->
      // If the current media item is configured for clipping then we need to jump
      // to the appropriately clipped time in the item without running into other parts of the file
      val seekTimeMs = (clipping.durationMs.toFloat() * progress).roundToLong() + clipping.startMs
      mediaPlayer.controls().setTime(seekTimeMs)
    } ?: run {
      mediaPlayer.controls().setPosition(progress)
    }
  }

  fun seekTo(index: Int) {
    if (index in mediaItems.indices) {
      currentItemIndex = index
      startCurrentItem()
    }
  }

  fun skipToNext() {
    if (currentItemIndex < mediaItems.size - 1) {
      currentItemIndex += 1
      startCurrentItem()
    }
  }

  fun skipToPrevious() {
    if (currentItemIndex > 0) {
      currentItemIndex -= 1
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

  @Deprecated("Migrate this to prepareCurrentItem")
  private fun startCurrentItem(vararg options: VlcOption) {
    mediaItems.getOrNull(currentItemIndex)?.also { item ->
      val wasHandled = listener?.onMediaItemChanged(item) == true

      if (mediaPlayer.isPlaying) {
        mediaPlayer.stop()
      }

      val opts = if (wasHandled) {
        arrayOf(*options, VlcOption.StartPaused)
      } else {
        options
      }.let {
        amendOptionsWithClipping(it.toList(), item)
      }.toOptionArray()

      val result = mediaPlayer.media().start(item.uri, *opts)
      if (!result) {
        bark(TAG) { "Unable to start playback for ${item.uri}" }
      } else {
        bark(TAG) { "Starting ${item.uri}, with ${opts.joinToString()}" }
      }
    }
  }

  private fun prepareCurrentItem(vararg options: VlcOption) {
    mediaItems.getOrNull(currentItemIndex)?.also { item ->
      listener?.onMediaItemChanged(item)

      if (mediaPlayer.isPlaying) {
        mediaPlayer.stop()
      }

      val opts = options
        .none { it is VlcOption.StartPaused }
        .let {
          if (it) arrayOf(*options, VlcOption.StartPaused)
          else options
        }.let {
          amendOptionsWithClipping(it.toList(), item)
        }.toOptionArray()

      val result = mediaPlayer.media().start(item.uri, *opts)
      if (!result) {
        bark(TAG) { "Unable to start playback for ${item.uri}" }
      } else {
        bark(TAG) { "Starting Paused ${item.uri}, with ${opts.joinToString()}" }
      }
    }
  }

  private fun amendOptionsWithClipping(
    options: List<VlcOption>,
    item: MediaItem,
  ): List<VlcOption> {
    return if (item.clipping != null) {
      val newOpts = options.toMutableList()
      // check if we have an existing start time
      val existingStartTime = options.firstOrNull { it is VlcOption.StartTime } as? VlcOption.StartTime
      if (existingStartTime == null || existingStartTime.milliseconds < item.clipping.startMs) {
        newOpts.removeIf { it is VlcOption.StartTime }
        newOpts += VlcOption.StartTime(item.clipping.startMs / 1000)
      }

      newOpts += VlcOption.EndTime(item.clipping.endMs / 1000)

      newOpts
    } else {
      options
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

      // If the current media item has a clipping configuration set then
      // we'll just want to report its duration since the underlying
      // VLC player will just report the entire duration of the media item that
      // likely spans the entire book
      val adjustedDuration = currentMediaItem.clipping?.let { clipping ->
        clipping.endMs - clipping.startMs
      } ?: newLength

      listener?.onDurationChanged(adjustedDuration)
    }

    override fun timeChanged(mediaPlayer: MediaPlayer?, newTime: Long) {
      bark(TAG) { "timeChanged(newTime=$newTime)" }

      // If the current media item has a clipping configuration set then
      // we'll want to adjust the current time here to be within the clip
      // since the underlying VLC player will report based on the entire
      // media item, i.e the one audio file for the entire book
      val adjustedTime = currentMediaItem.clipping?.let { clipping ->
        newTime - clipping.startMs
      } ?: newTime

      listener?.onPositionChanged(adjustedTime)
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

sealed class VlcOption(val option: String) {
  data object StartPaused : VlcOption("start-paused")
  data class StartTime(val seconds: Long) : VlcOption("start-time=${seconds}"){
    val milliseconds: Long get() = seconds * 1000
  }
  data class EndTime(val seconds: Long) : VlcOption("end-time=${seconds}")
}

private fun List<VlcOption>.toOptionArray(): Array<String> {
  return map { it.option }.toTypedArray()
}

private const val TAG = "VlcPlayer"

private const val OPTION_START_PAUSED = "start-paused"
