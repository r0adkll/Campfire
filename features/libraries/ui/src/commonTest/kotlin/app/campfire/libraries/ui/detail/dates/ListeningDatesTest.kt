// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class ListeningDatesTest {

  private val zone = TimeZone.of("America/New_York")

  @Test
  fun keepsTheTimeOfDayItFinishedAt() {
    val finishedAt = finishedAtOn(
      date = LocalDate(2026, 3, 28),
      previously = at(2026, 4, 1, 21, 15),
      startedAt = at(2026, 3, 3, 8, 0),
      now = at(2026, 5, 1, 12, 0),
      timeZone = zone,
    )

    assertThat(finishedAt).isEqualTo(at(2026, 3, 28, 21, 15))
  }

  @Test
  fun withNoEarlierFinish_finishesAtNoon() {
    val finishedAt = finishedAtOn(
      date = LocalDate(2026, 3, 28),
      previously = null,
      startedAt = 0L,
      now = at(2026, 5, 1, 12, 0),
      timeZone = zone,
    )

    assertThat(finishedAt).isEqualTo(at(2026, 3, 28, 12, 0))
  }

  @Test
  fun neverFinishesInTheFuture() {
    val now = at(2026, 5, 1, 9, 0)

    val finishedAt = finishedAtOn(
      date = LocalDate(2026, 5, 1),
      previously = at(2026, 4, 2, 22, 0),
      startedAt = at(2026, 3, 3, 8, 0),
      now = now,
      timeZone = zone,
    )

    assertThat(finishedAt).isEqualTo(now)
  }

  @Test
  fun neverFinishesBeforeItStarted() {
    val startedAt = at(2026, 3, 3, 20, 0)

    val finishedAt = finishedAtOn(
      date = LocalDate(2026, 3, 3),
      previously = at(2026, 4, 1, 7, 30),
      startedAt = startedAt,
      now = at(2026, 5, 1, 12, 0),
      timeZone = zone,
    )

    assertThat(finishedAt).isEqualTo(startedAt)
  }

  @Test
  fun aStartAfterNow_stillLeavesNowAsTheLatest() {
    val now = at(2026, 5, 1, 12, 0)

    val finishedAt = finishedAtOn(
      date = LocalDate(2026, 4, 30),
      previously = null,
      startedAt = at(2026, 6, 1, 12, 0),
      now = now,
      timeZone = zone,
    )

    assertThat(finishedAt).isEqualTo(now)
  }

  private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    LocalDateTime(year, month, day, hour, minute).toInstant(zone).toEpochMilliseconds()
}
