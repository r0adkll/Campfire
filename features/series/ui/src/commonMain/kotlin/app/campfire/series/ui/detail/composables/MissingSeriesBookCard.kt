// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.ItemCardInformation
import app.campfire.common.compose.widgets.placeholderBookPainter
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.cd_book_cover
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
    ItemCardInformation(
      title = entry.title,
      // Blank rather than null so the card keeps an empty line instead of "Unknown author".
      subtitle = seriesBookLabel(
        position = entry.position,
        year = entry.releaseDate?.take(4),
      ).orEmpty(),
    )
  }
}
