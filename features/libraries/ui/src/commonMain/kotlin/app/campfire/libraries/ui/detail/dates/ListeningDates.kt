// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** One of the dates a finished item's progress keeps of when it was listened to. */
enum class ListeningDate {
  Started,
  Finished,
}

/**
 * When an item finished on [date], at the time of day it [previously] finished so that only the
 * day changes, and held between when it was [startedAt] and [now] so the progress can't finish
 * before it began or in the future.
 */
internal fun finishedAtOn(
  date: LocalDate,
  previously: Long?,
  startedAt: Long,
  now: Long,
  timeZone: TimeZone = TimeZone.currentSystemDefault(),
): Long {
  val timeOfDay = previously
    ?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(timeZone).time }
    ?: NOON
  val finishedAt = LocalDateTime(date, timeOfDay).toInstant(timeZone).toEpochMilliseconds()
  return finishedAt.coerceIn(startedAt.coerceIn(0L, now), now)
}

private val NOON = LocalTime(hour = 12, minute = 0)
