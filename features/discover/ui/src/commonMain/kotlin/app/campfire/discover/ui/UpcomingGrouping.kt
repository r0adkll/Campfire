// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import app.campfire.discover.api.DiscoveredBook
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

/** A release month; null in [UpcomingGroup.key] marks the trailing TBA bucket. */
data class MonthKey(val year: Int, val month: Month)

data class UpcomingGroup(
  val key: MonthKey?,
  val books: List<DiscoveredBook>,
)

/**
 * Groups upcoming books into an ascending release timeline by month, with
 * books whose dates can't be parsed (or are absent) collected into a final
 * TBA group.
 */
fun groupUpcomingByMonth(books: List<DiscoveredBook>): List<UpcomingGroup> {
  val dated = mutableMapOf<MonthKey, MutableList<Pair<LocalDate, DiscoveredBook>>>()
  val tba = mutableListOf<DiscoveredBook>()

  for (book in books) {
    val date = parseReleaseDate(book.entry.releaseDate)
    if (date == null) {
      tba += book
    } else {
      dated.getOrPut(MonthKey(date.year, date.month)) { mutableListOf() } += date to book
    }
  }

  val groups = dated.entries
    .sortedWith(compareBy({ it.key.year }, { it.key.month.ordinal }))
    .map { (key, entries) ->
      UpcomingGroup(
        key = key,
        books = entries
          .sortedWith(compareBy({ it.first }, { it.second.entry.position ?: Double.MAX_VALUE }))
          .map { it.second },
      )
    }

  return if (tba.isEmpty()) groups else groups + UpcomingGroup(key = null, books = tba)
}

/** Provider dates are ISO-ish timestamps; the first ten chars are yyyy-MM-dd. */
fun parseReleaseDate(raw: String?): LocalDate? {
  val datePart = raw?.take(10) ?: return null
  return try {
    LocalDate.parse(datePart)
  } catch (e: IllegalArgumentException) {
    null
  }
}
