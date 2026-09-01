// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.discover.api.DiscoveredBook
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month

class UpcomingGroupingTest {

  private fun book(title: String, releaseDate: String?, position: Double = 1.0) = DiscoveredBook(
    seriesId = "s1",
    seriesName = "The Stormlight Archive",
    entry = ProviderSeriesEntry(
      providerBookId = "id-$title",
      position = position,
      title = title,
      releaseDate = releaseDate,
      isReleased = false,
      providerUrl = null,
      coverUrl = null,
    ),
    providerId = ProviderId.Audible,
  )

  @Test
  fun `books group by month in ascending order`() = run {
    val groups = groupUpcomingByMonth(
      listOf(
        book("December Book", "2027-12-01"),
        book("March Book", "2027-03-04"),
        book("Next Year Book", "2028-01-15"),
      ),
    )

    assertThat(groups.map { it.key }).isEqualTo(
      listOf(
        MonthKey(2027, Month.MARCH),
        MonthKey(2027, Month.DECEMBER),
        MonthKey(2028, Month.JANUARY),
      ),
    )
  }

  @Test
  fun `books within a month sort by day then position`() = run {
    val groups = groupUpcomingByMonth(
      listOf(
        book("Later", "2027-03-20"),
        book("Earlier Two", "2027-03-04", position = 2.0),
        book("Earlier One", "2027-03-04", position = 1.5),
      ),
    )

    assertThat(groups.single().books.map { it.entry.title })
      .isEqualTo(listOf("Earlier One", "Earlier Two", "Later"))
  }

  @Test
  fun `undated and unparseable dates land in a trailing tba group`() = run {
    val groups = groupUpcomingByMonth(
      listOf(
        book("No Date", null),
        book("Garbage Date", "soon-ish"),
        book("Year Only", "2027"),
        book("Dated", "2027-03-04"),
      ),
    )

    assertThat(groups.size).isEqualTo(2)
    assertThat(groups.first().key).isEqualTo(MonthKey(2027, Month.MARCH))
    assertThat(groups.last().key).isNull()
    assertThat(groups.last().books.map { it.entry.title })
      .isEqualTo(listOf("No Date", "Garbage Date", "Year Only"))
  }

  @Test
  fun `timestamps are trimmed to their date part`() = run {
    assertThat(parseReleaseDate("2027-03-04T00:00:00Z")).isEqualTo(LocalDate(2027, 3, 4))
    assertThat(parseReleaseDate("2027-03-04")).isEqualTo(LocalDate(2027, 3, 4))
    assertThat(parseReleaseDate(null)).isNull()
  }

  @Test
  fun `all undated books yield only the tba group`() = run {
    val groups = groupUpcomingByMonth(listOf(book("No Date", null)))

    assertThat(groups.size).isEqualTo(1)
    assertThat(groups.single().key).isNull()
  }
}
