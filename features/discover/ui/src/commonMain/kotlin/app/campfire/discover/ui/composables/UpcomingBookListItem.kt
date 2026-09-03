// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.discover.api.DiscoveredBook
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.cd_book_cover
import org.jetbrains.compose.resources.stringResource

/**
 * An upcoming release styled like
 * [app.campfire.common.compose.widgets.LibraryItemListItem] — same card,
 * thumbnail, and type treatment, with a release date where the library row
 * shows its duration. Rows without a provider URL render inert.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UpcomingBookListItem(
  book: DiscoveredBook,
  releaseDateLabel: String?,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.large
  ElevatedContentCard(
    onClick = onClick,
    shape = shape,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .clip(shape)
          .size(ThumbnailSize),
      ) {
        CoverImage(
          imageUrl = book.entry.coverUrl,
          contentDescription = stringResource(Res.string.cd_book_cover, book.entry.title),
          placeholder = placeholderBookPainter(),
          size = ThumbnailSize,
          shape = shape,
          contentScale = ContentScale.Crop,
        )
      }

      Spacer(Modifier.size(16.dp))

      Column(
        modifier = Modifier.weight(1f),
      ) {
        Text(
          text = book.entry.title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text = book.seriesName,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        if (releaseDateLabel != null) {
          Spacer(Modifier.size(4.dp))

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Icon(
              Icons.Outlined.Event,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = releaseDateLabel,
              style = MaterialTheme.typography.labelSmallEmphasized,
            )
          }
        }
      }

      Spacer(Modifier.size(8.dp))
    }
  }
}

private val ThumbnailSize = 88.dp
