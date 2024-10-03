package app.campfire.common.compose.extensions

import kotlin.time.Duration

fun Duration.readoutFormat(): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  return buildString {
    if (hours > 0) append("$hours hours ")
    if (minutes > 0) append("$minutes minutes ")
    if (seconds > 0) append("$seconds seconds")
    if (hours == 0L && minutes == 0L && seconds == 0L) append("nothing")
  }
}

fun Duration.clockFormat(): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  return buildString {
    if (hours > 0) append("$hours").append(":")
    append("$minutes".padStart(2, '0'))
      .append(":")
      .append("$seconds".padStart(2, '0'))
  }
}
