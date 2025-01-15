package app.campfire.audioplayer.impl.kvo

import app.campfire.audioplayer.impl.util.asCMTime
import app.campfire.audioplayer.impl.util.seconds
import kotlin.time.Duration
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.addBoundaryTimeObserverForTimes
import platform.AVFoundation.addPeriodicTimeObserverForInterval
import platform.AVFoundation.removeTimeObserver
import platform.AVFoundation.valueWithCMTime
import platform.Foundation.NSValue
import platform.darwin.dispatch_get_main_queue

class AVTimeObservable private constructor(
  private val avPlayer: AVPlayer,
) : AVTimeScope, AutoCloseable {

  private var observerToken: Any? = null

  @OptIn(ExperimentalForeignApi::class)
  override fun addPeriodicObserver(
    interval: Duration,
    action: (Duration) -> Unit,
  ) {
    close()
    observerToken = avPlayer.addPeriodicTimeObserverForInterval(
      interval = interval.asCMTime(),
      queue = dispatch_get_main_queue(),
    ) { cValueTime ->
      action(cValueTime.seconds)
    }
  }

  @OptIn(ExperimentalForeignApi::class)
  override fun addBoundariesObserver(
    times: List<Duration>,
    action: () -> Unit,
  ) {
    close()
    observerToken = avPlayer.addBoundaryTimeObserverForTimes(
      times = times.map { NSValue.valueWithCMTime(it.asCMTime()) },
      queue = dispatch_get_main_queue(),
      usingBlock = action,
    )
  }

  override fun close() {
    observerToken?.let { avPlayer.removeTimeObserver(it) }
  }

  companion object {

    fun AVPlayer.createTimeObservable(
      block: AVTimeScope.() -> Unit,
    ): AVTimeObservable {
      return AVTimeObservable(this).apply(block)
    }
  }
}

/**
 * Scoped interface to provide a limited and specific kotlin-ified way to
 * add timer observers to an [AVPlayer] instance
 */
interface AVTimeScope {

  fun addPeriodicObserver(
    interval: Duration,
    action: (Duration) -> Unit,
  )

  fun addBoundariesObserver(
    times: List<Duration>,
    action: () -> Unit,
  )
}
