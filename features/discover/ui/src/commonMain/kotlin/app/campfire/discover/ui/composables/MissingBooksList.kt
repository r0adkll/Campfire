// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.core.model.SeriesId
import app.campfire.discover.api.DiscoveredBook
import app.campfire.discover.ui.parseReleaseDate
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.cd_open_series
import campfire.features.discover.ui.generated.resources.discover_book_position
import campfire.features.discover.ui.generated.resources.discover_missing_count
import org.jetbrains.compose.resources.stringResource

private val MissingCardWidth = 150.dp

/**
 * Missing books as home-style shelves: a clickable series header over a
 * horizontal row of cards. Keys are series-scoped — a cross-listed book can
 * appear under two series.
 */
@Composable
internal fun MissingBooksList(
  books: List<DiscoveredBook>,
  onSeriesClick: (SeriesId, String) -> Unit,
  onBookClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val grouped = books.groupBy { it.seriesId }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(bottom = 24.dp),
  ) {
    grouped.forEach { (seriesId, seriesBooks) ->
      val seriesName = seriesBooks.first().seriesName
      item(key = "series:$seriesId") {
        SeriesHeaderRow(
          seriesName = seriesName,
          missingCount = seriesBooks.size,
          onClick = { onSeriesClick(seriesId, seriesName) },
        )
      }
      item(key = "shelf:$seriesId") {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
          items(
            items = seriesBooks,
            key = { "$seriesId:${it.entry.providerBookId}" },
          ) { book ->
            MissingBookCard(
              book = book,
              supportingText = listOfNotNull(
                book.entry.position?.let {
                  stringResource(Res.string.discover_book_position, formatPosition(it))
                },
                parseReleaseDate(book.entry.releaseDate)?.year?.toString(),
              ).joinToString(" · "),
              onClick = book.entry.providerUrl?.let { url -> { onBookClick(url) } },
              modifier = Modifier.width(MissingCardWidth),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SeriesHeaderRow(
  seriesName: String,
  missingCount: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = seriesName,
      style = MaterialTheme.typography.titleMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Text(
      text = stringResource(Res.string.discover_missing_count, missingCount),
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      modifier = Modifier.padding(start = 16.dp),
    )
    Icon(
      Icons.Rounded.ChevronRight,
      contentDescription = stringResource(Res.string.cd_open_series, seriesName),
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 4.dp),
    )
  }
}
