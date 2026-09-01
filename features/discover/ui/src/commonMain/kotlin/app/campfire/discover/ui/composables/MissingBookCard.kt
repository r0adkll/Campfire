// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.discover.api.DiscoveredBook
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.cd_book_cover
import org.jetbrains.compose.resources.stringResource

/**
 * A provider-listed book styled like the home screen's [app.campfire.common.compose.widgets.LibraryItemCard]
 * — square cover on top, title and supporting line below. Cards without a
 * provider URL render inert.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MissingBookCard(
  book: DiscoveredBook,
  supportingText: String,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  ElevatedContentCard(
    onClick = onClick,
    modifier = modifier,
  ) {
    CoverImage(
      imageUrl = book.entry.coverUrl,
      contentDescription = stringResource(Res.string.cd_book_cover, book.entry.title),
      shape = MaterialTheme.shapes.largeIncreased,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f),
    )
    Column(
      modifier = Modifier.padding(vertical = 16.dp),
    ) {
      Text(
        text = book.entry.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
  }
}
