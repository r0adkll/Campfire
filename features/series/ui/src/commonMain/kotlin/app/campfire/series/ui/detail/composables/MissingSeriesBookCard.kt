// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.placeholderBookPainter
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.cd_book_cover
import campfire.features.series.ui.generated.resources.missing_book_position
import org.jetbrains.compose.resources.stringResource

/**
 * A provider-listed book the user doesn't own, rendered as a grid cell in the
 * series listing under the missing header. Cards without a provider URL are
 * inert.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MissingSeriesBookCard(
  entry: ProviderSeriesEntry,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  ElevatedContentCard(
    onClick = onClick,
    modifier = modifier,
  ) {
    val scrim = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
    CoverImage(
      imageUrl = entry.coverUrl,
      contentDescription = stringResource(Res.string.cd_book_cover, entry.title),
      placeholder = placeholderBookPainter(),
      shape = MaterialTheme.shapes.largeIncreased,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f),
      sharedElementModifier = Modifier
        .drawWithContent {
          drawContent()
          drawRoundRect(scrim, cornerRadius = CornerRadius(20.dp.toPx()))
        },
    )
    Column(
      modifier = Modifier.padding(vertical = 16.dp),
    ) {
      Text(
        text = entry.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      Text(
        text = listOfNotNull(
          entry.position?.let { stringResource(Res.string.missing_book_position, formatPosition(it)) },
          entry.releaseDate?.take(4),
        ).joinToString(" · "),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
  }
}

/** "1", "1.5" — reading-order position without a trailing ".0". */
private fun formatPosition(position: Double): String {
  return if (position % 1.0 == 0.0) position.toInt().toString() else position.toString()
}
