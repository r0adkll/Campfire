package app.campfire.audioplayer.impl.player

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.impl.kvo.AVTimeObservable.Companion.createTimeObservable
import app.campfire.audioplayer.impl.kvo.NSObservable.Option.New
import app.campfire.audioplayer.impl.kvo.NSObservable.Option.Old
import app.campfire.audioplayer.impl.kvo.NSObservableAction
import app.campfire.audioplayer.impl.kvo.observe
import app.campfire.audioplayer.impl.mediaitem.IosMediaItem
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.util.ZERO_CM_TIME
import app.campfire.audioplayer.impl.util.asCMTime
import app.campfire.audioplayer.impl.util.asCMTimeFromMillis
import app.campfire.audioplayer.impl.util.asCMTimeSeconds
import app.campfire.audioplayer.impl.util.asDebugString
import app.campfire.audioplayer.impl.util.seconds
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
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
import platform.AVFoundation.asset
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.reasonForWaitingToPlay
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.setDefaultRate
import platform.AVFoundation.setRate
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTime
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.NSNotificationCenter

@OptIn(ExperimentalForeignApi::class)
class IosPlayer(
  private val skipToPreviousResetThreshold: Duration = 5.seconds,
) : AutoCloseable {

  private val closeables = mutableListOf<AutoCloseable>()
  private val playerItemCloseables = mutableListOf<AutoCloseable>()

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

  private var playWhenReady: Boolean = false
  private var isPreparing: Boolean = false

  //endregion

  private val playerItemStatusObserver: NSObservableAction<AVPlayerItem> = { path, playerItem, change ->
    bark(LogPriority.INFO) {
      "PlayerItemStatusObserver(keyPath=$path, change=$change, status=${
        avPlayerItemStatusString(
          playerItem.status,
        )
      })"
    }

    if (playerItem.status == AVPlayerItemStatusReadyToPlay) {
      // If marked to play immediately, start it now
      if (playWhenReady) {
        bark { "playWhenReady() -- Item Ready!" }
        playWhenReady = false
        avPlayer.play()
      }

      // Mark the preparing phase as completed and the time ready to reflect the actual AVPlayer/Item states
      isPreparing = false
    }

    syncPlayerState("playerItemStatusObserver()")
  }

  private val avPlayer = AVPlayer().apply {
    automaticallyWaitsToMinimizeStalling = true

    closeables += observe(
      path = "timeControlStatus",
      options = listOf(Old, New),
    ) { _, obj, change ->
      bark { "AVPlayer::timeControlStatus(change=$change): ${obj!!.rate}" }
      syncPlayerState("kvo::timeControlStatus")
    }

    closeables += observe(
      path = "rate",
      options = listOf(Old, New),
    ) { _, obj, change ->
      bark { "AVPlayer::rate(change=$change): ${obj!!.rate}" }
      syncPlayerState("kvo::rate")
    }

    closeables += NSNotificationCenter.defaultCenter.observe(
      name = AVAudioSessionInterruptionNotification,
      nsObject = AVAudioSession.sharedInstance(),
    ) { _, notification ->
      bark { "audioSession Interruption Notification: $notification" }
    }

    closeables += NSNotificationCenter.defaultCenter.observe(
      name = AVAudioSessionRouteChangeNotification,
      nsObject = AVAudioSession.sharedInstance(),
    ) { _, notification ->
      bark { "audioSession Route Change Notification: $notification" }
    }
  }

  fun setMediaItems(items: List<IosMediaItem>) {
    // Reset the media player
    reset()

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
      // Set the current state to preparing so we can adjust accordingly when we sync the current AVPlayer
      // and AVPlayerItem statuses to the outgoing [_state] flow
      isPreparing = true
      syncPlayerState("prepare(start)")

      // Ensure player is reset
      removeCurrentItemObservers()
      avPlayer.pause()
      avPlayer.replaceCurrentItemWithPlayerItem(null)

      // Determine the current media item position based on passed time information
      bark {
        "IosPlayer(currentItemIndex=$currentItemIndex, " +
          "startTimeInItemMillis=$startTimeInItemMillis, " +
          "playImmediately=$playImmediately)"
      }

      // Now grab media item and compute the starting offset within the item
      val mediaItem = mediaItems[currentItemIndex]
      val track = mediaItem.indexedTrackAtItemPosition(startTimeInItemMillis.milliseconds)?.second

      // Pre-populate state information
      val overallTime = mediaItem.startOffset + startTimeInItemMillis.milliseconds
      _currentDuration.value = track?.duration ?: 0.seconds
      _currentPosition.value = track?.timeInTrack(overallTime) ?: 0.seconds
      _currentMetadata.value = track?.metadata
      _overallPosition.value = overallTime

      playWhenReady = playImmediately

      // Now initialize the iOS player with said info
      val avPlayerItem = mediaItem.asAVPlayerItem().apply {
        playerItemCloseables += observe(
          path = "status",
          options = listOf(Old, New),
          action = playerItemStatusObserver,
        )

        asset.loadValuesAsynchronouslyForKeys(listOf("duration")) {
          bark { "Player item duration loaded: ${asset.duration.seconds}" }
          scheduleNextSkipOnEndPlaying(duration = asset.duration)
        }
      }

      avPlayer.replaceCurrentItemWithPlayerItem(avPlayerItem)

      // TODO: Is this actually needed per item? I feel like we could just scope this to
      //  the object.
      startTimeObserver()

      if (startTimeInItemMillis > 0L) {
        bark { "--> seekWhenReady($startTimeInItemMillis) -- Item Ready!" }
        avPlayer.seekToTime(startTimeInItemMillis.asCMTimeFromMillis()) { completed ->
          bark { "<-- Seek Completion: $completed" }
        }
      }
    } else {
      throw IllegalStateException("No media items have been set, or the current item is out of index")
    }
  }

  private fun scheduleNextSkipOnEndPlaying(duration: CValue<CMTime>) {
    playerItemCloseables += avPlayer.createTimeObservable {
      addBoundariesObserver(
        times = listOf(duration.seconds),
      ) {
        bark { "Time boundary reached, skipping to next item" }
        onCurrentItemFinished()
      }
    }
  }

  private fun startTimeObserver() {
    playerItemCloseables += avPlayer.createTimeObservable {
      addPeriodicObserver(0.5.seconds, ::onUpdate)
    }
  }

  private fun removeCurrentItemObservers() {
    playerItemCloseables.forEach(AutoCloseable::close)
    playerItemCloseables.clear()
  }

  fun playPause() {
    bark { "playPause(${avTimeControlStatusString(avPlayer.timeControlStatus)})" }
    when (avPlayer.timeControlStatus) {
      AVPlayerTimeControlStatusPlaying -> avPlayer.pause()
      AVPlayerTimeControlStatusPaused -> avPlayer.play()
    }
  }

  fun pause() {
    avPlayer.pause()
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
      avPlayer.seekToTime(startTimeInItemMillis.asCMTimeFromMillis())
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
      avPlayer.seekToTime(nextTrack.startMs.asCMTimeFromMillis())
      avPlayer.playIfReady()
    } else {
      // Treat the current item as finished, and start the next one
      // or end the playback.
      onCurrentItemFinished()
    }
  }

  fun skipToPrevious() {
    val currentTimeInItem = avPlayer.currentTime().seconds
    val (index, track) = currentMediaItem.indexedTrackAtItemPosition(currentTimeInItem)
      ?: throw IllegalStateException("Unable to determine current track in player")

    if (index > 0) {
      if (currentTimeInItem > skipToPreviousResetThreshold) {
        val trackStartTimeInItem = (track.startMs - currentMediaItem.startOffset.inWholeMilliseconds)
          .coerceAtLeast(0L)
        // If we are well into the playback for the current track, just seek to the start of the track
        avPlayer.seekToTime(trackStartTimeInItem.asCMTimeFromMillis())
        avPlayer.playIfReady()
      } else {
        // The previous track exists, so just seek to its start time
        val nextTrack = currentMediaItem.tracks[index - 1]
        avPlayer.seekToTime(nextTrack.startMs.asCMTimeFromMillis())
        avPlayer.playIfReady()
      }
    } else if (currentItemIndex > 0) {
      if (currentTimeInItem > skipToPreviousResetThreshold) {
        avPlayer.seekToTime(ZERO_CM_TIME)
        avPlayer.playIfReady()
      } else {
        // The previous track would be in the previous item, seek to that item
        currentItemIndex--

        // Seed our state with the change while things prepare
        onUpdate(0.seconds)
        _state.value = AudioPlayer.State.Buffering

        prepare(playImmediately = true)
      }
    } else if (avPlayer.status == AVPlayerStatusReadyToPlay) {
      // If the previous is the start of the item, just seek to the start if the player is ready
      avPlayer.seekToTime(ZERO.asCMTime())
      avPlayer.playIfReady()
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

  override fun close() {
    closeables.forEach(AutoCloseable::close)
    closeables.clear()

    reset()
  }

  /**
   * Reset the audio player, removing all current item and time observers,
   * pausing the player and clearing the current item.
   */
  private fun reset() {
    removeCurrentItemObservers()
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
    // Get the current MediaItem and Track for the current position in the playing media item.
    // Then update the current track metadata
    val currentItem = currentMediaItem
    val (_, track) = currentItem.indexedTrackAtItemPosition(timeInItem) ?: return

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

      // Prepare the visual state for the transition
      onUpdate(0.seconds)
      _state.value = AudioPlayer.State.Buffering

      prepare(playImmediately = true)
    } else {
      // TODO: We are in a "Finished" state at this point. Add "Finished" to the list of available
      //  [AudioPlayer.State] options.
      reset()
    }
  }

  /**
   * Sync the current state of [avPlayer] to the [state] flow so that listeners of this player
   * can be updated with the current player state
   */
  private fun syncPlayerState(tag: String) {
    // If we are actively preparing the player than we should be in a buffering state,
    // since we can't actively rely on the status of the AV objects during this phase
    if (isPreparing) {
      _state.value = AudioPlayer.State.Buffering
      bark {
        """
          SyncPlayerState(
            tag = $tag,
            isPreparing = true,
          ) ==> Buffering
        """.trimIndent()
      }
      return
    }

    _state.value = when (avPlayer.status) {
      AVPlayerStatusReadyToPlay -> when (avPlayer.currentItem?.status) {
        AVPlayerItemStatusReadyToPlay -> when (avPlayer.timeControlStatus) {
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

        else -> AudioPlayer.State.Disabled
      }

      AVPlayerStatusFailed -> AudioPlayer.State.Disabled
      AVPlayerStatusUnknown -> AudioPlayer.State.Disabled
      // This should never be reached, but is needed due to iOS translation layer
      else -> AudioPlayer.State.Disabled
    }.also {
      bark {
        """
        SyncPlayerState(
          tag = $tag,
          playerStatus = ${avPlayerStatusString(avPlayer.status)},
          timeControlStatus = ${avTimeControlStatusString(avPlayer.timeControlStatus)},
          currentItem.status = ${avPlayerItemStatusString(avPlayer.currentItem?.status)},
          currentItem.failure = ${avPlayer.currentItem?.error?.asDebugString()},
          error = ${avPlayer.error},
        ) ==> $it
        """.trimIndent()
      }
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
