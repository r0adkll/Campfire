package app.campfire.audioplayer.impl.player

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.vlcj.ifPlayable
import app.campfire.audioplayer.impl.vlcj.isPlaying
import app.campfire.audioplayer.impl.vlcj.pause
import app.campfire.audioplayer.impl.vlcj.play
import app.campfire.audioplayer.impl.vlcj.seekBackward
import app.campfire.audioplayer.impl.vlcj.seekForward
import app.campfire.audioplayer.impl.vlcj.stop
import app.campfire.core.logging.Cork
import app.campfire.core.util.runIf
import kotlin.math.floor
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

  private var didFinish: Boolean = false

  var volume: Float
    get() = mediaPlayer.audio().volume() / 100f
    set(value) {
      // the floor() is important here as our fade controller works in small float increments.
      // without this operation the first increment always ends up rounding back to 100
      mediaPlayer.audio().setVolume(floor(value * 100).toInt())
    }

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

  fun getMediaItemCount(): Int = mediaItems.size

  fun getMediaItemAt(index: Int): MediaItem {
    return mediaItems[index]
  }

  fun setCurrentItem(index: Int) {
    if (index in mediaItems.indices) {
      currentItemIndex = index
    }
  }

  fun prepare(
    playImmediately: Boolean,
    startTimeInItemMillis: Long = 0L,
  ) {
    if (mediaItems.isNotEmpty() && currentItemIndex in mediaItems.indices) {
      prepareCurrentItem(playImmediately, startTimeInItemMillis)
    } else {
      throw IllegalStateException("No media items have been set, or the current item is out of index")
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

  fun seekTo(index: Int, startTimeInItemMillis: Long = 0L) {
    if (index in mediaItems.indices) {
      currentItemIndex = index
      prepareCurrentItem(startTimeInItemMillis = startTimeInItemMillis)
    }
  }

  fun skipToNext() {
    if (currentItemIndex < mediaItems.size - 1) {
      currentItemIndex += 1
      prepareCurrentItem()
    } else {
      // We are finished!
      didFinish = true
      listener?.onFinished()
    }
  }

  fun skipToPrevious() {
    if (currentItemIndex > 0) {
      currentItemIndex -= 1
      prepareCurrentItem()
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

  /**
   * Prepare the current [MediaItem] for the [currentItemIndex] with playback options
   * as playing immediately and the start offset
   * @param playImmediately whether or not playback should start immediately after the item is prepared
   * @param startTimeInItemMillis the start time offset relative to the media item
   */
  private fun prepareCurrentItem(
    playImmediately: Boolean = true,
    startTimeInItemMillis: Long = 0L,
  ) {
    mediaItems.getOrNull(currentItemIndex)?.also { item ->
      val wasHandled = listener?.onMediaItemChanged(item) == true

      didFinish = false

      if (mediaPlayer.isPlaying) {
        mediaPlayer.stop()
      }

      val options = buildOptions(
        item = item,
        playImmediately = playImmediately && !wasHandled,
        startTimeInItemMillis = startTimeInItemMillis,
      )

      val result = mediaPlayer.media().start(item.uri, *options)
      if (result) {
        dbark { "Starting '${item.metadata?.title}', with ${options.joinToString()}" }
      } else {
        dbark { "Unable to start playback for '${item.metadata?.title}'" }
      }
    }
  }

  private fun buildOptions(
    item: MediaItem,
    playImmediately: Boolean,
    startTimeInItemMillis: Long,
  ): Array<out String> {
    return buildList {
      if (!playImmediately) add(VlcOption.StartPaused)

      item.clipping?.let { clipping ->
        val startTimeMs = clipping.startMs + startTimeInItemMillis
        add(VlcOption.StartTime(startTimeMs / 1000L))
        add(VlcOption.StopTime(clipping.endMs / 1000L))
      } ?: runIf(startTimeInItemMillis > 0L) {
        add(VlcOption.StartTime(startTimeInItemMillis / 1000L))
      }
    }.toOptionArray()
  }

  interface Listener {
    fun onStateChanged(state: AudioPlayer.State)
    fun onDurationChanged(durationInMillis: Long)
    fun onPositionChanged(positionInMillis: Long)
    fun onMediaItemChanged(mediaItem: MediaItem): Boolean
    fun onFinished()
  }

  private inner class EventListener : MediaPlayerEventAdapter() {
    override fun mediaPlayerReady(mediaPlayer: MediaPlayer?) {
      dbark { "mediaPlayerReady()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun mediaChanged(mediaPlayer: MediaPlayer?, media: MediaRef?) {
      dbark { "mediaChanged($media)" }
      mediaPlayer?.syncStateToListener()
    }

    override fun opening(mediaPlayer: MediaPlayer?) {
      dbark { "opening()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun buffering(mediaPlayer: MediaPlayer?, newCache: Float) {
      dbark { "buffering(newCache=$newCache)" }
      mediaPlayer?.syncStateToListener()
    }

    override fun playing(mediaPlayer: MediaPlayer?) {
      dbark { "playing" }
      mediaPlayer?.syncStateToListener()
    }

    override fun paused(mediaPlayer: MediaPlayer?) {
      dbark { "paused()" }
      mediaPlayer?.syncStateToListener()
    }

    override fun stopped(mediaPlayer: MediaPlayer?) {
      dbark { "stopped()" }
//      mediaPlayer?.syncStateToListener()
    }

    override fun forward(mediaPlayer: MediaPlayer?) {
      dbark { "forward()" }
    }

    override fun backward(mediaPlayer: MediaPlayer?) {
      dbark { "backward()" }
    }

    override fun finished(mediaPlayer: MediaPlayer?) {
      dbark { "finished($mediaPlayer)" }
      listener?.onPositionChanged(0L)
      mediaPlayer?.submit {
        skipToNext()
        mediaPlayer.syncStateToListener()
      }
    }

    override fun seekableChanged(mediaPlayer: MediaPlayer?, newSeekable: Int) {
      dbark { "seekableChanged(newSeekable=$newSeekable)" }
    }

    override fun pausableChanged(mediaPlayer: MediaPlayer?, newPausable: Int) {
      dbark { "pausableChanged(newPausable=$newPausable)" }
    }

    override fun titleChanged(mediaPlayer: MediaPlayer?, newTitle: Int) {
      dbark { "titleChanged(newTitle=$newTitle)" }
    }

    override fun lengthChanged(mediaPlayer: MediaPlayer?, newLength: Long) {
      dbark { "lengthChanged(newLength=$newLength)" }

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
      dbark { "timeChanged(newTime=$newTime)" }

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
      dbark { "corked(corked=$corked)" }
    }

    override fun muted(mediaPlayer: MediaPlayer?, muted: Boolean) {
      dbark { "muted(muted=$muted)" }
    }

    override fun audioDeviceChanged(mediaPlayer: MediaPlayer?, audioDevice: String?) {
      dbark { "audioDeviceChanged(audioDevice=$audioDevice)" }
    }

    override fun chapterChanged(mediaPlayer: MediaPlayer?, newChapter: Int) {
      dbark { "chapterChanged(newChapter=$newChapter)" }
    }

    override fun error(mediaPlayer: MediaPlayer?) {
      dbark { "error(${mediaPlayer?.status()?.state()})" }
      mediaPlayer?.syncStateToListener()
    }

    private fun MediaPlayer.syncStateToListener() {
      val state = status().state() ?: State.NOTHING_SPECIAL
      ibark { "VlcPlayer State: ${state.name}, didFinish=$didFinish" }
      val playerState = when (state) {
        State.PLAYING -> AudioPlayer.State.Playing
        State.PAUSED -> AudioPlayer.State.Paused

        State.OPENING,
        State.BUFFERING,
        -> AudioPlayer.State.Buffering

        State.ENDED -> AudioPlayer.State.Finished

        State.STOPPED -> if (didFinish) AudioPlayer.State.Finished else AudioPlayer.State.Disabled

        State.ERROR,
        State.NOTHING_SPECIAL,
        -> AudioPlayer.State.Disabled
      }
      listener?.onStateChanged(playerState)
    }
  }

  companion object : Cork {
    override val tag: String = "VlcPlayer"
    override val enabled: Boolean = true
  }
}

sealed class VlcOption(val option: String) {
  data object StartPaused : VlcOption(":start-paused")
  data class StartTime(val seconds: Long) : VlcOption(":start-time=$seconds")
  data class StopTime(val seconds: Long) : VlcOption(":stop-time=$seconds")
}

private fun List<VlcOption>.toOptionArray(): Array<String> {
  return map { it.option }.toTypedArray()
}
