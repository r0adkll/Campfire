// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.common.compose.widgets.CoverImage
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.series_entry_missing
import campfire.features.series.ui.generated.resources.series_entry_upcoming
import campfire.features.series.ui.generated.resources.series_entry_upcoming_year
import org.jetbrains.compose.resources.stringResource

/**
 * A book in the series the user doesn't have, rendered as a dimmed "ghost" of a
 * [app.campfire.common.compose.widgets.LibraryItemCard] so it reads as absent
 * without competing with owned books. Deliberately does not register shared
 * element bounds — there's no detail screen to transition into.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProviderEntryCard(
  entry: ProviderSeriesEntry,
  isUpcoming: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedCard(
    onClick = onClick,
    shape = MaterialTheme.shapes.largeIncreased,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .padding(8.dp),
    ) {
      CoverImage(
        imageUrl = entry.coverUrl,
        contentDescription = entry.title,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
          .fillMaxWidth()
          .alpha(0.45f),
      )
    }

    Column(
      modifier = Modifier.padding(horizontal = 12.dp),
    ) {
      Text(
        text = entry.title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(2.dp))
      Text(
        text = when {
          !isUpcoming -> stringResource(Res.string.series_entry_missing)
          entry.releaseYear() != null ->
            stringResource(Res.string.series_entry_upcoming_year, entry.releaseYear()!!)
          else -> stringResource(Res.string.series_entry_upcoming)
        },
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        maxLines = 1,
      )
      Spacer(Modifier.height(12.dp))
    }
  }
}

private fun ProviderSeriesEntry.releaseYear(): String? {
  return releaseDate?.take(4)?.takeIf { it.length == 4 && it.all(Char::isDigit) }
}
