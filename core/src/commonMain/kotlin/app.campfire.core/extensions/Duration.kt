package app.campfire.core.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Return the progress as a [Float] in range of (0f..1f) over [other] duration.
 *
 * @receiver the numerator duration to calculate
 * @param other the denominator duration to calculate
 * @return the progress as a [Float] in range of (0f..1f)
 */
infix fun Duration.progressOver(other: Duration): Float {
  if (other == 0.milliseconds) return 0f
  return div(other).toFloat()
}
