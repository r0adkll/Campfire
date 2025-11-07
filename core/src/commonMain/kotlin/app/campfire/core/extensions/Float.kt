package app.campfire.core.extensions

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

val Float.seconds: Duration
  get() = toDouble().seconds

/**
 * Return the float receiver as a string display with numOfDec after the decimal (rounded)
 * (e.g. 35.72 with numOfDec = 1 will be 35.7, 35.78 with numOfDec = 2 will be 35.80)
 *
 * @param numOfDec number of decimal places to show (receiver is rounded to that number)
 * @return the String representation of the receiver up to numOfDec decimal places
 */
fun Float.toString(numOfDec: Int): String {
  val integerDigits = this.toInt()
  val floatDigits = ((this - integerDigits) * 10f.pow(numOfDec)).roundToInt()
  return "$integerDigits.$floatDigits"
}
