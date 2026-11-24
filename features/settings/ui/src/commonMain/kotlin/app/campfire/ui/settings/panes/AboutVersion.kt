// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.ui.settings.panes

import app.campfire.core.app.ApplicationInfo
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.char
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toLocalDateTime

private const val SEPARATOR = " · "

private val CommitTimeFormat = LocalDateTime.Format {
  year()
  char('-')
  monthNumber()
  char('-')
  day()
  char(' ')
  hour()
  char(':')
  minute()
}

// Not UtcOffset's ISO format, which writes a zero offset as "Z".
private val OffsetFormat = UtcOffset.Format {
  offsetHours()
  char(':')
  offsetMinutesOfHour()
}

/** `1.4.0 (1040099)` */
internal val ApplicationInfo.readableVersion: String
  get() = "$versionName ($versionCode)"

/**
 * `2026-11-24 09:03 -05:00 · 220c0be` — the commit this build came from, in [timeZone]. Either half
 * is dropped when unknown; null when both are.
 */
internal fun ApplicationInfo.commitLine(timeZone: TimeZone = TimeZone.currentSystemDefault()): String? {
  val time = commitTime?.let { instant ->
    val local = instant.toLocalDateTime(timeZone)
    "${CommitTimeFormat.format(local)} ${OffsetFormat.format(timeZone.offsetAt(instant))}"
  }
  return listOfNotNull(time, commitSha).joinToString(SEPARATOR).ifEmpty { null }
}

/** What a long-press on Version copies: [readableVersion] plus the [commitLine] when known. */
internal fun ApplicationInfo.versionCopyText(timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
  listOfNotNull(readableVersion, commitLine(timeZone)).joinToString(SEPARATOR)
