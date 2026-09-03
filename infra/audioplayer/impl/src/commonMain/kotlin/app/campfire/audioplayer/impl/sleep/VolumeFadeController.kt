// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.sleep

import kotlin.math.pow
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object VolumeFadeController {

  /**
   * The attenuation, in decibels, that the fade reaches right before it snaps to silence.
   *
   * Player volume is a linear gain, but hearing is logarithmic: a linear ramp sounds nearly
   * full volume for most of its length and then falls off a cliff. Sweeping the gain linearly
   * in dB instead halves the perceived loudness every ~10 dB, so a 40 dB sweep sounds like a
   * steady fade across the whole duration.
   */
  internal const val FADE_FLOOR_DB = -40f

  /**
   * The linear gain multiplier at [progress] (0f..1f) through the fade, following a dB-linear
   * curve from unity down to [FADE_FLOOR_DB].
   */
  internal fun gainAt(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return 10f.pow(FADE_FLOOR_DB * p / 20f)
  }

  /**
   * Fade the player from its current volume to silence over [duration], then call [onPause] and
   * restore the original volume. A zero [duration] pauses immediately.
   *
   * The curve is driven by elapsed wall time rather than a fixed per-tick decrement so a slow or
   * delayed tick never stretches the fade past [duration].
   */
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
      val totalMillis = duration.inWholeMilliseconds

      val start = now()
      while (isActive && getVolume() > 0f) {
        val elapsed = now() - start
        if (elapsed >= totalMillis) break
        setVolume(startVolume * gainAt(elapsed.toFloat() / totalMillis))
        delay(delayStep)
      }

      setVolume(0f)
      onPause()

      // Reset the volume to where it started
      setVolume(startVolume)
    }
  }
}
