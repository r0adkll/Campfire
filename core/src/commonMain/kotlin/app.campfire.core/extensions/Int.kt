package app.campfire.core.extensions

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Int.formattedPlace(): String = when (this) {
  1 -> "${this}st"
  2 -> "${this}nd"
  3 -> "${this}rd"
  else -> "${this}th"
}

fun Long.asDate(): LocalDate = Instant.fromEpochMilliseconds(this)
  .toLocalDateTime(TimeZone.currentSystemDefault())
  .date
