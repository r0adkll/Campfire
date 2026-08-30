package app.campfire.common.compose.extensions

import kotlin.math.roundToInt

fun Double.roundToSingleDecimal(): String {
  return ((this * 10).roundToInt() / 10.0).toString()
}
