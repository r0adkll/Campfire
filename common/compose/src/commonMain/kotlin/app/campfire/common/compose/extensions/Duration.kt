package app.campfire.common.compose.extensions

import kotlin.time.Duration
import kotlin.time.DurationUnit

enum class ReadoutStyle {
  Long, Short
}

fun ReadoutStyle.formatForStyle(short: String, long: String): String = when (this) {
  ReadoutStyle.Long -> long
  ReadoutStyle.Short -> short
}

fun Duration.readoutAtMost(
  atMost: DurationUnit,
  style: ReadoutStyle = ReadoutStyle.Short,
): String {
  return if (inWholeHours > 0 && atMost >= DurationUnit.HOURS) {
    "$inWholeHours ${style.formatForStyle("hrs", "hours")}"
  } else if (inWholeMinutes > 0 && atMost >= DurationUnit.MINUTES) {
    "$inWholeMinutes ${style.formatForStyle("min", "minutes")}"
  } else if (inWholeSeconds > 0 && atMost >= DurationUnit.SECONDS) {
    "$inWholeSeconds ${style.formatForStyle("s", "seconds")}"
  } else if (inWholeMilliseconds > 0 && atMost >= DurationUnit.MILLISECONDS) {
    "$inWholeMilliseconds ${style.formatForStyle("ms", "millis")}"
  } else if (inWholeMicroseconds > 0 && atMost >= DurationUnit.MICROSECONDS) {
    "$inWholeMicroseconds ${style.formatForStyle("μs", "micros")}"
  } else {
    "$inWholeNanoseconds ${style.formatForStyle("ns", "nanos")}"
  }
}

fun Duration.readoutFormat(
  style: ReadoutStyle = ReadoutStyle.Short,
  largestOnly: Boolean = false,
): String {
  val hours = inWholeHours
  val minutes = inWholeMinutes % 60
  val seconds = inWholeSeconds % 60

  return buildString {
    if (hours > 0) {
      append("$hours${style.formatForStyle("h", "hours")} ")
      if (largestOnly) return@buildString
    }
    if (minutes > 0) {
      append("$minutes${style.formatForStyle("m", "minutes")} ")
      if (largestOnly) return@buildString
    }
    if (seconds > 0) {
      append("$seconds${style.formatForStyle("s", "seconds")}")
      if (largestOnly) return@buildString
    }
    if (hours == 0L && minutes == 0L && seconds == 0L) append("--")
  }
}

fun Duration.thresholdReadoutFormat(
  thresholds: Map<DurationUnit, Int> = mapOf(
    DurationUnit.MINUTES to 180, // 3hrs
    DurationUnit.HOURS to 72, // 3 days
  ),
): String = buildString {
  fun thresholdFor(unit: DurationUnit): Int {
    return thresholds.getOrElse(unit) {
      when (unit) {
        DurationUnit.SECONDS -> 60
        DurationUnit.MINUTES -> 60
        DurationUnit.HOURS -> 24
        else -> 0
      }
    }
  }

  if (inWholeMinutes == 0L) {
    append("${inWholeSeconds}s")
  } else if (inWholeMinutes < thresholdFor(DurationUnit.MINUTES)) {
    append("${inWholeMinutes}m")
  } else if (inWholeHours < thresholdFor(DurationUnit.HOURS)) {
    val remainingMinutes = inWholeMinutes % 60
    append("${inWholeHours}h")
    if (remainingMinutes > 0) {
      append(" ")
      append("${remainingMinutes}m")
    }
  } else {
    val remainingHours = inWholeHours % 24
    append("${inWholeDays}d")
    if (remainingHours > 0) {
      append(" ")
      append("${remainingHours}h")
    }
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

fun Duration.largestDurationUnit(): DurationUnit {
  DurationUnit.entries.reversed().forEach { unit ->
    val rawValue = toDouble(unit)
    if (rawValue > 1.0) {
      return unit
    }
  }
  // if for some reason we can't determine, return the smallest unit
  return DurationUnit.NANOSECONDS
}
