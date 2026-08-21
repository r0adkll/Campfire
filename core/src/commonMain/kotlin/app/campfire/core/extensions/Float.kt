// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val Float.seconds: Duration
  get() = toDouble().seconds

/**
 * Return the float receiver as a string display with numOfDec after the decimal (rounded, zero-padded)
 * (e.g. 35.72 with numOfDec = 1 will be 35.7, 1.03 with numOfDec = 2 will be 1.03, 1.999 with numOfDec = 2 will be 2.00)
 *
 * @param numOfDec number of decimal places to show (receiver is rounded to that number)
 * @return the String representation of the receiver up to numOfDec decimal places
 */
fun Float.toString(numOfDec: Int): String {
  if (isNaN() || isInfinite()) return "--"
  require(numOfDec >= 0) { "numOfDec must be >= 0" }
  val scale = 10.0.pow(numOfDec)
  // Round the whole value at the requested scale so carries propagate into the integer part (0.96 -> 1.0)
  val scaled = (abs(this).toDouble() * scale).roundToLong()
  val integerDigits = (scaled / scale.toLong()).toString()
  val sign = if (this < 0f && scaled != 0L) "-" else ""
  if (numOfDec == 0) return "$sign$integerDigits"
  val fractionDigits = (scaled % scale.toLong()).toString().padStart(numOfDec, '0')
  return "$sign$integerDigits.$fractionDigits"
}

/**
 * Round the receiver to the nearest hundredth, scrubbing floating point noise
 * (e.g. 1.1500001 -> 1.15).
 */
fun Float.roundToHundredths(): Float = (this * 100f).roundToLong() / 100f

/**
 * Shortest display form of the receiver at tenths precision, but keeping a trailing 5 in the
 * hundredths place (e.g. 1 -> "1", 1.5 -> "1.5", 1.25 -> "1.25", 1.23 -> "1.2").
 */
val Float.readable: String
  get() {
    val hundredths = (this * 100f).roundToLong()
    return when {
      hundredths % 100L == 0L -> toString(0)
      hundredths % 10L == 0L -> toString(1)
      hundredths % 5L == 0L -> toString(2)
      else -> toString(1)
    }
  }

/**
 * Shortest display form of the receiver at hundredths precision
 * (e.g. 1 -> "1", 1.1 -> "1.1", 1.03 -> "1.03", 1.25 -> "1.25").
 */
val Float.readableHundredths: String
  get() {
    val hundredths = (this * 100f).roundToLong()
    return when {
      hundredths % 100L == 0L -> toString(0)
      hundredths % 10L == 0L -> toString(1)
      else -> toString(2)
    }
  }
