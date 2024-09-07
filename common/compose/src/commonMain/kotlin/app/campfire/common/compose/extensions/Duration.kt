package app.campfire.common.compose.extensions

import kotlin.time.Duration

fun Duration.readoutFormat(): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  return "${hours}h ${minutes}min ${seconds}s"
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
