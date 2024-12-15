package app.campfire.common.compose.extensions

import kotlin.time.Duration

enum class ReadoutStyle {
  Long, Short
}

fun Duration.readoutFormat(style: ReadoutStyle = ReadoutStyle.Short): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  fun formatForStyle(short: String, long: String): String = when (style) {
    ReadoutStyle.Long -> long
    ReadoutStyle.Short -> short
  }

  return buildString {
    if (hours > 0) append("$hours${formatForStyle("h", "hours")} ")
    if (minutes > 0) append("$minutes${formatForStyle("m", "minutes")} ")
    if (seconds > 0) append("$seconds${formatForStyle("s", "seconds")}")
    if (hours == 0L && minutes == 0L && seconds == 0L) append("--")
  }
}

fun Duration.clockFormat(): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  return buildString {
    if (hours > 0) {
      append("$hours")
        .append(":")
        .append("$minutes".padStart(2, '0'))
    } else {
      append("$minutes")
    }
      .append(":")
      .append("$seconds".padStart(2, '0'))
  }
}
