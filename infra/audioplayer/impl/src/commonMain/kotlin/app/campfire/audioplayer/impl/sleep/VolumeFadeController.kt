package app.campfire.audioplayer.impl.sleep

import app.campfire.core.extensions.asSeconds
import app.campfire.core.logging.bark
import kotlin.time.Duration
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object VolumeFadeController {

  fun fade(
    scope: CoroutineScope,
    duration: Duration,
    tickRate: Long,
    getVolume: () -> Float,
    setVolume: (Float) -> Unit,
    onPause: () -> Unit,
  ): Job {
    return scope.launch {
      val delayStep = 1000L / tickRate
      val fadeStep = getVolume() / (duration.asSeconds() * tickRate)

      val elapsed = measureTime {
        while (isActive && getVolume() > 0f) {
          setVolume((getVolume() - fadeStep).coerceAtLeast(0f))
          delay(delayStep)
        }
      }

      bark(tag = "VolumeFadeController") { "Fade took $elapsed to complete" }

      onPause()
    }
  }
}
