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
   * Fade playback to silence over [duration], then call [onPause] and release the fade.
   * A zero [duration] pauses immediately.
   *
   * [setFade] receives a *multiplier*, 1f down to 0f, that the caller composes with whatever else
   * is attenuating output — see [app.campfire.audioplayer.impl.volume.OutputGain]. The fade never
   * learns the user's volume and never writes an absolute gain, so a slider moved mid-fade is
   * neither clobbered on the way down nor undone when the fade releases.
   *
   * The curve is driven by elapsed wall time rather than a fixed per-tick decrement so a slow or
   * delayed tick never stretches the fade past [duration]. Cancelling the returned job releases
   * the fade without pausing, so a listener who resumes mid-fade keeps listening.
   */
  fun fade(
    scope: CoroutineScope,
    duration: Duration,
    tickRate: Long,
    setFade: (Float) -> Unit,
    onPause: () -> Unit,
    now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
  ): Job {
    return scope.launch {
      val delayStep = 1000L / tickRate
      val totalMillis = duration.inWholeMilliseconds

      val start = now()
      try {
        while (isActive) {
          val elapsed = now() - start
          if (elapsed >= totalMillis) break
          setFade(gainAt(elapsed.toFloat() / totalMillis))
          delay(delayStep)
        }

        setFade(0f)
        onPause()
      } finally {
        // Release the fade, whether it finished or was interrupted. Playback is paused by now in
        // the finished case, so this restores the level for whenever it resumes.
        setFade(1f)
      }
    }
  }
}
