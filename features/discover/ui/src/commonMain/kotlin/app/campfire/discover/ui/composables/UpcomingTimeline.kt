// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui.composables

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.extensions.capitalized
import app.campfire.discover.api.DiscoveredBook
import app.campfire.discover.ui.MonthKey
import app.campfire.discover.ui.groupUpcomingByMonth
import app.campfire.discover.ui.parseReleaseDate
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.discover_tba_group
import org.jetbrains.compose.resources.stringResource

/**
 * Upcoming releases as an ascending month-by-month timeline, with undated
 * entries in a trailing "to be announced" group.
 */
@Composable
internal fun UpcomingTimeline(
  books: List<DiscoveredBook>,
  onBookClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val groups = groupUpcomingByMonth(books)

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    groups.forEach { group ->
      item(key = "month:${group.key?.let { "${it.year}-${it.month.ordinal}" } ?: "tba"}") {
        MonthHeader(key = group.key)
      }
      group.books.forEach { book ->
        item(key = "${group.key}:${book.seriesId}:${book.entry.providerBookId}") {
          val releaseDay = parseReleaseDate(book.entry.releaseDate)
            ?.let { "${it.month.name.capitalized()} ${it.day}" }
          DiscoveredBookRow(
            book = book,
            supportingText = listOfNotNull(book.seriesName, releaseDay).joinToString(" · "),
            onBookClick = onBookClick,
          )
        }
      }
    }
  }
}

@Composable
private fun MonthHeader(
  key: MonthKey?,
  modifier: Modifier = Modifier,
) {
  Text(
    text = key
      ?.let { "${it.month.name.capitalized()} ${it.year}" }
      ?: stringResource(Res.string.discover_tba_group),
    style = MaterialTheme.typography.titleMedium,
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
  )
}
