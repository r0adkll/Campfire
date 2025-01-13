package app.campfire.audioplayer.impl.player

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.mediaitem.IosMediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.util.asCMTime
import app.campfire.audioplayer.impl.util.asCMTimeSeconds
import app.campfire.audioplayer.impl.util.seconds
import app.campfire.core.logging.bark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerItemStatusUnknown
import platform.AVFoundation.AVPlayerStatusFailed
import platform.AVFoundation.AVPlayerStatusReadyToPlay
import platform.AVFoundation.AVPlayerStatusUnknown
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVPlayerWaitingDuringInterstitialEventReason
import platform.AVFoundation.AVPlayerWaitingForCoordinatedPlaybackReason
import platform.AVFoundation.AVPlayerWaitingToMinimizeStallsReason
import platform.AVFoundation.AVPlayerWaitingWhileEvaluatingBufferingRateReason
import platform.AVFoundation.AVPlayerWaitingWithNoItemToPlayReason
import platform.AVFoundation.addBoundaryTimeObserverForTimes
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.asset
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.reasonForWaitingToPlay
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setDefaultRate
import platform.AVFoundation.setRate
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.valueWithCMTime
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.Foundation.NSKeyValueObservingOptionNew
import platform.Foundation.NSValue
import platform.Foundation.addObserver
import platform.darwin.NSObject
import platform.darwin.dispatch_get_main_queue
import platform.foundation.NSKeyValueObservingProtocol

@OptIn(ExperimentalForeignApi::class)
class IosPlayer {

  //region State Information

  private val _state = MutableStateFlow(AudioPlayer.State.Disabled)
  val state = _state.asStateFlow()

  private val _currentPosition = MutableStateFlow(0.seconds)
  val currentPosition = _currentPosition.asStateFlow()

  private val _overallPosition = MutableStateFlow(0.seconds)
  val overallPosition = _overallPosition.asStateFlow()

  private val _currentDuration = MutableStateFlow(0.seconds)
  val currentDuration = _currentDuration.asStateFlow()

  private val _currentMetadata = MutableStateFlow<MediaItem.Metadata?>(null)
  val currentMetadata = _currentMetadata.asStateFlow()

  //endregion

  //region Media Items

  private val mediaItems = ArrayDeque<IosMediaItem>(20)

  var currentItemIndex = 0

  val currentMediaItem: IosMediaItem
    get() = mediaItems[currentItemIndex]

  //endregion

  private val timeControlObserver: NSObject = object : NSObject(), NSKeyValueObservingProtocol {
    override fun observeValueForKeyPath(
      keyPath: String?,
      ofObject: Any?,
      change: Map<Any?, *>?,
      context: COpaquePointer?,
    ) {
      // bark(LogPriority.INFO) { "TimeControlObserver(keyPath=$keyPath, ofObject=$ofObject, change=$change)" }
      syncPlayerState()
    }
  }

  private val playerItemStatusObserver: NSObject = object : NSObject(), NSKeyValueObservingProtocol {
    override fun observeValueForKeyPath(
      keyPath: String?,
      ofObject: Any?,
      change: Map<Any?, *>?,
      context: COpaquePointer?,
    ) {
//      bark(LogPriority.INFO) { "PlayerItemStatusObserver(keyPath=$keyPath, ofObject=$ofObject, change=$change)" }
      syncPlayerState()
    }
  }

  private val avPlayer = AVPlayer().apply {
    addObserver(
      observer = timeControlObserver,
      forKeyPath = "timeControlStatus",
      options = NSKeyValueObservingOptionNew,
      context = null,
    )
  }

  private var timeObserverToken: Any? = null

  fun setMediaItems(items: List<IosMediaItem>) {
    // Reset the media player
    release()

    // Reset our internal data structure
    mediaItems.clear()
    mediaItems.addAll(items)
    currentItemIndex = 0
  }

  fun getMediaItemCount(): Int = mediaItems.size

  fun getMediaItemAt(index: Int): IosMediaItem {
    return mediaItems[index]
  }

  /**
   * Prepare this player for playback, feeding it an option to start playback immediately when its ready
   * or to wait. Also give the starting overall time position of where to resume/start playback.
   * @param playImmediately start playback immediately
   * @param startTimeInItemMillis the start time relative to the [currentMediaItem]
   */
  fun prepare(
    playImmediately: Boolean,
    startTimeInItemMillis: Long = 0L,
  ) {
    if (mediaItems.isNotEmpty()) {
      // Ensure player is reset
      avPlayer.pause()
      avPlayer.replaceCurrentItemWithPlayerItem(null)
      stopTimeObserver()

      // Determine the current media item position based on passed time information

      // Now grab media item and compute the starting offset within the item
      val mediaItem = mediaItems[currentItemIndex]
      val track = mediaItem.indexedTrackAtItemPosition(startTimeInItemMillis.milliseconds)?.second

      // Pre-populate state information
      _currentDuration.value = track?.duration ?: 0.seconds
      _currentPosition.value = track?.timeInTrack(startTimeInItemMillis.milliseconds) ?: 0.seconds
      _overallPosition.value = mediaItem.startOffset + startTimeInItemMillis.milliseconds

      // Now initialize the iOS player with said info
      val avPlayerItem = mediaItem.asAVPlayerItem().apply {
        addObserver(
          observer = playerItemStatusObserver,
          forKeyPath = "status",
          options = NSKeyValueObservingOptionNew,
          null,
        )

        asset.loadValuesAsynchronouslyForKeys(listOf("duration")) {
          scheduleNextSkipOnEndPlaying(duration = asset.duration)
        }
      }

      avPlayer.replaceCurrentItemWithPlayerItem(avPlayerItem)
      if (startTimeInItemMillis > 0L) {
        avPlayer.seekToTime(startTimeInItemMillis.asCMTimeSeconds())
      }

      startTimeObserver()

      if (playImmediately) {
        avPlayer.play()
      }

      // Update our current state information
      syncPlayerState()
    } else {
      throw IllegalStateException("No media items have been set, or the current item is out of index")
    }
  }

  private fun scheduleNextSkipOnEndPlaying(duration: CValue<CMTime>) {
    val time = CMTimeMakeWithSeconds(seconds = CMTimeGetSeconds(duration), preferredTimescale = 1)
    timeObserverToken = avPlayer.addBoundaryTimeObserverForTimes(
      times = listOf(NSValue.valueWithCMTime(time)),
      queue = dispatch_get_main_queue(),
    ) {
      onCurrentItemFinished()
    }
  }

  private fun startTimeObserver() {
    timeObserverToken = avPlayer.addPeriodicTimeObserverForInterval((0.5).seconds.asCMTime(), null) {
      onUpdate(it.seconds)
    }
  }

  private fun stopTimeObserver() {
    timeObserverToken?.let {
      avPlayer.removeTimeObserver(it)
      timeObserverToken = null
    }
  }

  fun playPause() {
    when (avPlayer.timeControlStatus) {
      AVPlayerTimeControlStatusPlaying -> avPlayer.pause()
      AVPlayerTimeControlStatusPaused -> avPlayer.play()
    }
    syncPlayerState()
  }

  fun pause() {
    avPlayer.pause()
    syncPlayerState()
  }

  fun seekTo(trackId: Int) {
    for (index in mediaItems.indices) {
      val item = mediaItems[index]
      val track = item.tracks.find { it.id == trackId }
      if (track != null) {
        val startTimeInItemMillis = track.startMs - item.startOffset.inWholeMilliseconds
        seekTo(index, startTimeInItemMillis)
        return
      }
    }
    throw IllegalStateException("Unable to find track for Id($trackId)")
  }

  fun seekTo(progress: Float) {
    avPlayer.currentItem?.let { item ->
      val newTime = CMTimeGetSeconds(item.duration) * progress
      avPlayer.seekToTime(newTime.asCMTimeSeconds())
    }
  }

  fun seekTo(timestamp: Duration) {
    val timestampInMillis = timestamp.inWholeMilliseconds
    var mediaItemOffsetMs = 0L

    for (index in mediaItems.indices) {
      val mediaItem = mediaItems[index]
      val mediaItemDuration = mediaItem.duration.inWholeMilliseconds
      val mediaItemEnd = mediaItemOffsetMs + mediaItemDuration
      if (timestampInMillis in mediaItemOffsetMs until mediaItemEnd) {
        val progressInMediaItem = timestampInMillis - mediaItemOffsetMs
        seekTo(index, progressInMediaItem)
        return
      }
      mediaItemOffsetMs = mediaItemEnd
    }
  }

  private fun seekTo(index: Int, startTimeInItemMillis: Long) {
    if (index == currentItemIndex) {
      avPlayer.seekToTime(startTimeInItemMillis.asCMTimeSeconds())
    } else {
      currentItemIndex = index
      prepare(
        playImmediately = true,
        startTimeInItemMillis = startTimeInItemMillis,
      )
    }
  }

  fun skipToNext() {
    val currentTimeInItem = avPlayer.currentTime().seconds
    val (index, _) = currentMediaItem.indexedTrackAtItemPosition(currentTimeInItem)
      ?: throw IllegalStateException("Unable to determine current track in player")

    if (index < currentMediaItem.tracks.lastIndex) {
      // The next track exists, so just seek to its start time
      val nextTrack = currentMediaItem.tracks[index + 1]
      avPlayer.seekToTime(nextTrack.startMs.asCMTimeSeconds())
    } else {
      // Treat the current item as finished, and start the next one
      // or end the playback.
      onCurrentItemFinished()
    }
  }

  fun skipToPrevious() {
    val currentTimeInItem = avPlayer.currentTime().seconds
    val (index, _) = currentMediaItem.indexedTrackAtItemPosition(currentTimeInItem)
      ?: throw IllegalStateException("Unable to determine current track in player")

    if (index > 0) {
      // The previous track exists, so just seek to its start time
      val nextTrack = currentMediaItem.tracks[index - 1]
      avPlayer.seekToTime(nextTrack.startMs.asCMTimeSeconds())
    } else if (currentItemIndex > 0) {
      // The previous track would be in the previous item, seek to that item
      currentItemIndex--
      prepare(playImmediately = true)
    } else if (avPlayer.status == AVPlayerStatusReadyToPlay) {
      // If the previous is the start of the item, just seek to the start if the player is ready
      avPlayer.seekToTime(ZERO.asCMTime())
    }
  }

  fun seekForward(millis: Long) {
    val newTime = avPlayer.currentTime().seconds + millis.milliseconds
    avPlayer.seekToTime(newTime.asCMTime())
  }

  fun seekBackward(millis: Long) {
    val newTime = avPlayer.currentTime().seconds - millis.milliseconds
    avPlayer.seekToTime(newTime.asCMTime())
  }

  fun setPlaybackSpeed(rate: Float) {
    if (rate !in 0f..1f) return
    avPlayer.setDefaultRate(rate)
    if (avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying) {
      avPlayer.setRate(rate)
    }
  }

  fun release() {
    stopTimeObserver()
    avPlayer.pause()
    avPlayer.replaceCurrentItemWithPlayerItem(null)

    _state.value = AudioPlayer.State.Disabled
    _currentPosition.value = ZERO
    _overallPosition.value = ZERO
    _currentDuration.value = ZERO
    _currentMetadata.value = null
  }

  /**
   * Called by periodic time observer to update the state from the player
   */
  private fun onUpdate(timeInItem: Duration) {
    // Sync the current player state
    syncPlayerState()

    // Get the current MediaItem and Track for the current position in the playing media item.
    // Then update the current track metadata
    val currentItem = currentMediaItem
    val (_, track) = currentItem.indexedTrackAtItemPosition(timeInItem) ?: return

    bark {
      "onUpdate(timeInItem = $timeInItem, " +
        "trackStart = ${track.startMs}, " +
        "currentPosition = ${track.timeInTrack(timeInItem)}, " +
        "overallPosition=${currentItem.startOffset + timeInItem}, " +
        "metadata=${track.metadata.title})"
    }

    // Update stateful information based on track and position
    val trackNormalizedTime = timeInItem + currentItem.startOffset
    _currentPosition.value = track.timeInTrack(trackNormalizedTime)
    _overallPosition.value = currentItem.startOffset + timeInItem
    _currentDuration.value = track.duration
    _currentMetadata.value = track.metadata
  }

  private fun onCurrentItemFinished() {
    if (currentItemIndex < mediaItems.lastIndex) {
      currentItemIndex++
      prepare(playImmediately = true)
    } else {
      // TODO: We are in a "Finished" state at this point. Add "Finished" to the list of available
      //  [AudioPlayer.State] options.
      release()
      syncPlayerState()
    }
  }

  /**
   * Sync the current state of [avPlayer] to the [state] flow so that listeners of this player
   * can be updated with the current player state
   */
  private fun syncPlayerState() {
//    bark {
//      """
//        SyncPlayerState(
//          playerStatus = ${avPlayerStatusString(avPlayer.status)},
//          timeControlStatus = ${avTimeControlStatusString(avPlayer.timeControlStatus)},
//          currentItem.status = ${avPlayerItemStatusString(avPlayer.currentItem?.status)},
//          currentItem.failure = ${avPlayer.currentItem?.error?.asDebugString()},
//          error = ${avPlayer.error},
//        )
//      """.trimIndent()
//    }
    _state.value = when (avPlayer.status) {
      AVPlayerStatusReadyToPlay -> when (avPlayer.timeControlStatus) {
        AVPlayerTimeControlStatusPaused -> AudioPlayer.State.Paused
        AVPlayerTimeControlStatusPlaying -> AudioPlayer.State.Playing
        AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> when (avPlayer.reasonForWaitingToPlay) {
          AVPlayerWaitingWhileEvaluatingBufferingRateReason -> AudioPlayer.State.Buffering
          AVPlayerWaitingToMinimizeStallsReason -> AudioPlayer.State.Buffering
          AVPlayerWaitingForCoordinatedPlaybackReason -> AudioPlayer.State.Buffering
          AVPlayerWaitingDuringInterstitialEventReason -> AudioPlayer.State.Buffering
          AVPlayerWaitingWithNoItemToPlayReason -> AudioPlayer.State.Disabled
          // This should never be reached, but is needed due to iOS translation layer
          else -> AudioPlayer.State.Buffering
        }
        // This should never be reached, but is needed due to iOS translation layer
        else -> AudioPlayer.State.Disabled
      }

      AVPlayerStatusFailed -> AudioPlayer.State.Disabled
      AVPlayerStatusUnknown -> AudioPlayer.State.Disabled
      // This should never be reached, but is needed due to iOS translation layer
      else -> AudioPlayer.State.Disabled
    }
  }
}

fun avPlayerStatusString(status: Long): String = when (status) {
  AVPlayerStatusReadyToPlay -> "AVPlayerStatusReadyToPlay"
  AVPlayerStatusFailed -> "AVPlayerStatusFailed"
  AVPlayerStatusUnknown -> "AVPlayerStatusUnknown"
  else -> "<$status:unknown>"
}

fun avTimeControlStatusString(status: Long): String = when (status) {
  AVPlayerTimeControlStatusPaused -> "AVPlayerTimeControlStatusPaused"
  AVPlayerTimeControlStatusPlaying -> "AVPlayerTimeControlStatusPlaying"
  AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> "AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate"
  else -> "<$status: unknown>"
}

fun avPlayerItemStatusString(status: Long?) = when (status) {
  AVPlayerItemStatusReadyToPlay -> "AVPlayerItemStatusReadyToPlay"
  AVPlayerItemStatusFailed -> "AVPlayerItemStatusFailed"
  AVPlayerItemStatusUnknown -> "AVPlayerItemStatusUnknown"
  else -> "<$status: unknown>"
}

fun avReasonForWaitingToPlayString(reason: String): String = when (reason) {
  AVPlayerWaitingWhileEvaluatingBufferingRateReason -> "AVPlayerWaitingWhileEvaluatingBufferingRateReason"
  AVPlayerWaitingToMinimizeStallsReason -> "AVPlayerWaitingToMinimizeStallsReason"
  AVPlayerWaitingForCoordinatedPlaybackReason -> "AVPlayerWaitingForCoordinatedPlaybackReason"
  AVPlayerWaitingDuringInterstitialEventReason -> "AVPlayerWaitingDuringInterstitialEventReason"
  AVPlayerWaitingWithNoItemToPlayReason -> "AVPlayerWaitingWithNoItemToPlayReason"
  else -> "<unknown --> $reason>"
}
