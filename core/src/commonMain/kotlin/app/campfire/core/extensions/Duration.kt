package app.campfire.core.extensions

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

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

/**
 * Return the duration as a [Float] in seconds.
 */
fun Duration.asSeconds(): Float = toDouble(DurationUnit.SECONDS).toFloat()

infix fun ClosedRange<Duration>.isIn(other: ClosedRange<Duration>): Boolean {
  return start >= other.start && endInclusive <= other.endInclusive
}
