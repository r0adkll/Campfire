package app.campfire.core.extensions

import kotlin.time.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

val LocalDateTime.readableFormat: String
  get() {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val hourAdjusted = hour % 12
    val amPM = if (hour < 12) "AM" else "PM"
    return if (now.date == date) {
      "$hourAdjusted:$minute $amPM"
    } else {
      "${month.name.capitalized()} $day, $year $hourAdjusted:${minute.withSignificantZero()} $amPM"
    }
  }

val LocalDateTime.epochMilliseconds: Long
  get() = toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

val LocalDateTime.utcEpochMilliseconds: Long
  get() = toInstant(TimeZone.UTC).toEpochMilliseconds()

private fun Int.withSignificantZero(): String = if (this < 10) "0$this" else "$this"
