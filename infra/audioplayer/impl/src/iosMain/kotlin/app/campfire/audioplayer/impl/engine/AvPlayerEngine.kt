// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.impl.kvo.observe
import app.campfire.audioplayer.impl.mediaitem.MediaItem
import app.campfire.audioplayer.impl.util.asCMTime
import app.campfire.audioplayer.impl.util.durationOrNull
import app.campfire.core.logging.Cork
import app.campfire.core.logging.LogPriority
import app.campfire.core.logging.bark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.AVFoundation.AVAudioTimePitchAlgorithmTimeDomain
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeErrorKey
import platform.AVFoundation.AVPlayerItemFailedToPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVURLAsset
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.audioTimePitchAlgorithm
import platform.AVFoundation.automaticallyWaitsToMinimizeStalling
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.defaultRate
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.rate
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.replaceCurrentItemWithPlayerItem
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.AVFoundation.volume
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSError
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSThread
import platform.Foundation.NSURL
import platform.darwin.NSObjectProtocol
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * A [PlaybackEngine] over a single [AVPlayer], playing one [MediaItem] (a whole audio file) at a
 * time for [EngineAudioPlayer][app.campfire.audioplayer.impl.EngineAudioPlayer].
 *
 * Threading: the owner drives this from the main thread (its engine dispatcher on iOS), which is
 * also where every AVFoundation callback is funnelled ([onMain]), so engine state needs no locks.
 *
 * Authentication: [open] attaches the owner's request headers (bearer token and any access-proxy
 * headers) to every HTTP request the asset makes via `AVURLAssetHTTPHeaderFieldsKey`, so stream
 * URLs carry no credentials of their own.
 *
 * Opening an item is a small state machine: the item loads ([opening]), then it seeks to the
 * requested start, and only then is it reported ready ([EngineState.Paused] or playback starts).
 * Position ticks are held back while opening, during a seek and after the item ended, so the owner
 * never sees a stale position that would read as a chapter boundary crossing.
 */
@OptIn(ExperimentalForeignApi::class)
class AvPlayerEngine : PlaybackEngine {

  private val _events = MutableSharedFlow<PlaybackEngineEvent>(extraBufferCapacity = EVENT_BUFFER)
  override val events: Flow<PlaybackEngineEvent> = _events.asSharedFlow()

  override val supportsRequestHeaders: Boolean = true

  private val player = AVPlayer().apply {
    automaticallyWaitsToMinimizeStalling = true
  }

  override var volume: Float = 1f
    set(value) {
      field = value
      player.volume = value
    }

  private var rate = 1f

  private var currentItem: AVPlayerItem? = null
  private val itemObservers = mutableListOf<AutoCloseable>()
  private val notificationTokens = mutableListOf<NSObjectProtocol>()

  /** The item is loading; it isn't reported ready until it has seeked to [pendingStart]. */
  private var opening = false
  private var pendingStart = Duration.ZERO
  private var playWhenReady = false
  private var seeksInFlight = 0

  /** The item played to its end; state changes until the next [open] are AVPlayer winding down. */
  private var ended = false

  private val timeControlObserver = player.observe(path = "timeControlStatus") { _, _, _ ->
    onMain(::onTimeControlStatusChanged)
  }

  private val periodicObserver: Any? = player.addPeriodicTimeObserverForInterval(
    interval = POSITION_INTERVAL.asCMTime(),
    queue = dispatch_get_main_queue(),
  ) { time ->
    if (opening || ended || seeksInFlight > 0 || currentItem == null) return@addPeriodicTimeObserverForInterval
    time.durationOrNull()?.let { emit(PlaybackEngineEvent.PositionChanged(it)) }
  }

  override fun open(item: MediaItem, startPosition: Duration, playWhenReady: Boolean, headers: Map<String, String>) {
    clearItem()

    val url = item.uri.toNSUrl()
    if (url == null) {
      emit(PlaybackEngineEvent.Error(PlaybackEngineException("Unable to parse the media URL")))
      return
    }

    opening = true
    ended = false
    pendingStart = startPosition.coerceAtLeast(Duration.ZERO)
    this.playWhenReady = playWhenReady
    emit(PlaybackEngineEvent.StateChanged(EngineState.Opening))

    val options: Map<Any?, Any?>? = headers.takeIf { it.isNotEmpty() }?.let { mapOf(HTTP_HEADER_FIELDS_KEY to it) }
    val asset = AVURLAsset.URLAssetWithURL(url, options)
    val playerItem = AVPlayerItem.playerItemWithAsset(asset).apply {
      // Apple's recommended algorithm for spoken word at non-1x rates
      audioTimePitchAlgorithm = AVAudioTimePitchAlgorithmTimeDomain
    }
    currentItem = playerItem

    itemObservers += playerItem.observe(path = "status") { _, _, _ ->
      onMain { onItemStatusChanged(playerItem) }
    }
    notificationTokens += observeItemNotification(AVPlayerItemDidPlayToEndTimeNotification, playerItem) {
      if (playerItem !== currentItem) return@observeItemNotification
      ended = true
      emit(PlaybackEngineEvent.StateChanged(EngineState.Ended))
    }
    notificationTokens += observeItemNotification(AVPlayerItemFailedToPlayToEndTimeNotification, playerItem) { info ->
      if (playerItem !== currentItem) return@observeItemNotification
      val error = info?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
      fail("Playback stopped before the end of the item", error)
    }

    player.replaceCurrentItemWithPlayerItem(playerItem)
  }

  override fun play() {
    playWhenReady = true
    if (opening || currentItem == null) return
    player.defaultRate = rate
    player.play()
  }

  override fun pause() {
    playWhenReady = false
    player.pause()
  }

  override fun stop() {
    clearItem()
    player.replaceCurrentItemWithPlayerItem(null)
    emit(PlaybackEngineEvent.StateChanged(EngineState.Idle))
  }

  override fun seekTo(position: Duration) {
    val target = position.coerceAtLeast(Duration.ZERO)
    if (opening) {
      pendingStart = target
      return
    }
    val item = currentItem ?: return
    ended = false
    seek(item, target) { finished ->
      if (finished && seeksInFlight == 0) {
        player.currentTime().durationOrNull()?.let { emit(PlaybackEngineEvent.PositionChanged(it)) }
      }
    }
  }

  override fun setRate(rate: Float) {
    this.rate = rate
    player.defaultRate = rate
    if (player.timeControlStatus != AVPlayerTimeControlStatusPaused) {
      player.rate = rate
    }
  }

  override fun setEqualizer(enabled: Boolean, preampDb: Float, bandGainsDb: List<Float>) {
    // AVPlayer has no equalizer hook; the owner reports EqualizerState.Unsupported on iOS
  }

  override fun release() {
    stop()
    timeControlObserver.close()
    periodicObserver?.let(player::removeTimeObserver)
  }

  private fun onItemStatusChanged(item: AVPlayerItem) {
    if (item !== currentItem) return
    when (item.status) {
      AVPlayerItemStatusReadyToPlay -> if (opening) finishOpening(item)
      AVPlayerItemStatusFailed -> fail("Unable to load the media item", item.error)
    }
  }

  /** Seek to the requested start before reporting the item ready, so the first tick is the real position. */
  private fun finishOpening(item: AVPlayerItem) {
    item.duration.durationOrNull()
      ?.takeIf { it > Duration.ZERO }
      ?.let { emit(PlaybackEngineEvent.DurationChanged(it)) }

    val start = pendingStart
    if (start <= Duration.ZERO) {
      onOpened(item)
    } else {
      seek(item, start) { onOpened(item) }
    }
  }

  private fun onOpened(item: AVPlayerItem) {
    if (item !== currentItem || !opening) return
    opening = false
    player.currentTime().durationOrNull()?.let { emit(PlaybackEngineEvent.PositionChanged(it)) }
    if (playWhenReady) {
      player.defaultRate = rate
      player.play()
    } else {
      emit(PlaybackEngineEvent.StateChanged(EngineState.Paused))
    }
  }

  private fun onTimeControlStatusChanged() {
    if (opening || ended || currentItem == null) return
    val state = when (player.timeControlStatus) {
      AVPlayerTimeControlStatusPlaying -> EngineState.Playing
      AVPlayerTimeControlStatusPaused -> EngineState.Paused
      else -> EngineState.Buffering
    }
    emit(PlaybackEngineEvent.StateChanged(state))
  }

  private fun seek(item: AVPlayerItem, position: Duration, onComplete: (Boolean) -> Unit) {
    seeksInFlight++
    player.seekToTime(
      time = position.asCMTime(),
      toleranceBefore = kCMTimeZero.readValue(),
      toleranceAfter = kCMTimeZero.readValue(),
    ) { finished ->
      onMain {
        seeksInFlight = (seeksInFlight - 1).coerceAtLeast(0)
        if (item === currentItem) onComplete(finished)
      }
    }
  }

  private fun fail(message: String, error: NSError?) {
    opening = false
    val failure = error?.asPlaybackFailure(message) ?: PlaybackEngineException(message)
    bark(LogPriority.ERROR) { failure.message.orEmpty() }
    emit(PlaybackEngineEvent.Error(failure))
  }

  private fun clearItem() {
    itemObservers.forEach(AutoCloseable::close)
    itemObservers.clear()
    notificationTokens.forEach(NSNotificationCenter.defaultCenter::removeObserver)
    notificationTokens.clear()
    // Forget the item before pausing: the pause's timeControlStatus KVO fires synchronously and
    // would otherwise report a transient Paused for an item that is being replaced
    currentItem = null
    opening = false
    ended = false
    seeksInFlight = 0
    player.pause()
  }

  private fun observeItemNotification(
    name: String?,
    item: AVPlayerItem,
    action: (Map<Any?, *>?) -> Unit,
  ): NSObjectProtocol = NSNotificationCenter.defaultCenter.addObserverForName(
    name = name,
    `object` = item,
    queue = NSOperationQueue.mainQueue,
  ) { notification -> action(notification?.userInfo) }

  private fun emit(event: PlaybackEngineEvent) {
    if (!_events.tryEmit(event)) {
      bark(LogPriority.WARN) { "Dropped engine event $event" }
    }
  }

  private fun String.toNSUrl(): NSURL? = when {
    startsWith("/") -> NSURL.fileURLWithPath(this)
    else -> NSURL.URLWithString(this)
  }

  private companion object : Cork {
    override val tag: String = "AvPlayerEngine"

    /** Not exported as a symbol, but documented in AVFoundation's headers and widely relied on. */
    const val HTTP_HEADER_FIELDS_KEY = "AVURLAssetHTTPHeaderFieldsKey"

    val POSITION_INTERVAL = 500.milliseconds
    const val EVENT_BUFFER = 64

    fun onMain(block: () -> Unit) {
      if (NSThread.isMainThread) block() else dispatch_async(dispatch_get_main_queue(), block)
    }
  }
}
