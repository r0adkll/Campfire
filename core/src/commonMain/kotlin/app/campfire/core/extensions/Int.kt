// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.extensions

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Int.formattedPlace(): String = when (this) {
  1 -> "${this}st"
  2 -> "${this}nd"
  3 -> "${this}rd"
  else -> "${this}th"
}

fun Long.asDateTime(): LocalDateTime = Instant.fromEpochMilliseconds(this)
  .toLocalDateTime(TimeZone.currentSystemDefault())

fun Long.asDate(): LocalDate = asDateTime().date

/**
 * Human readable binary size with one decimal place (e.g. 1536 -> "1.5 KB", 10 MiB -> "10.0 MB").
 */
fun Long.asReadableBytes(): String {
  val kb = this.toDouble() / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  val tb = gb / 1024.0

  return if (tb >= 1.0) {
    tb.toFloat().toString(1) + " TB"
  } else if (gb >= 1.0) {
    gb.toFloat().toString(1) + " GB"
  } else if (mb >= 1.0) {
    mb.toFloat().toString(1) + " MB"
  } else if (kb >= 1.0) {
    kb.toFloat().toString(1) + " KB"
  } else {
    "$this B"
  }
}
