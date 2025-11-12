package app.campfire.core.extensions

import kotlin.time.Instant
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

fun Long.asReadableBytes(): String {
  val kb = this.toDouble() / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  val tb = gb / 1024.0

  return if (tb >= 1.0) {
    tb.toFloat().toString(2) + " TB"
  } else if (gb >= 1.0) {
    gb.toFloat().toString(2) + " GB"
  } else if (mb >= 1.0) {
    mb.toFloat().toString(2) + " MB"
  } else if (kb >= 1.0) {
    kb.toFloat().toString(2) + " KB"
  } else {
    "$this B"
  }
}
