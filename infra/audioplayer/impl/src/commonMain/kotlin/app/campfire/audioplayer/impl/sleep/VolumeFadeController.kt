package app.campfire.audioplayer.impl.sleep

import app.campfire.core.extensions.asSeconds
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

object VolumeFadeController {

  fun fade(
    scope: CoroutineScope,
    duration: Duration,
    tickRate: Long,
    getVolume: () -> Float,
    setVolume: (Float) -> Unit,
    onPause: () -> Unit,
    now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
  ): Job {
    return scope.launch {
      val startVolume = getVolume()
      val delayStep = 1000L / tickRate
      val fadeStep = getVolume() / (duration.asSeconds() * tickRate)

      // Grab a timestamp and never let the loop here extend passed the [duration]
      val start = now()
      while (isActive && getVolume() > 0f && now() - start < duration.inWholeMilliseconds) {
        setVolume((getVolume() - fadeStep).coerceAtLeast(0f))
        delay(delayStep)
      }

      onPause()

      // Reset the volume to where it started
      setVolume(startVolume)
    }
  }
}
