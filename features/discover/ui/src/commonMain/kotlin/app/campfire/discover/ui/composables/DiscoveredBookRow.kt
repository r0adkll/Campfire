// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.ui.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.discover.api.DiscoveredBook
import campfire.features.discover.ui.generated.resources.Res
import campfire.features.discover.ui.generated.resources.cd_book_cover
import org.jetbrains.compose.resources.stringResource

/**
 * One provider-listed book, shared by the missing and upcoming lists. Rows with
 * a provider URL open it; the rest render inert.
 */
@Composable
internal fun DiscoveredBookRow(
  book: DiscoveredBook,
  supportingText: String,
  onBookClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .let { base ->
        book.entry.providerUrl
          ?.let { url -> base.clickable { onBookClick(url) } }
          ?: base
      }
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CoverImage(
      imageUrl = book.entry.coverUrl,
      contentDescription = stringResource(Res.string.cd_book_cover, book.entry.title),
      size = 48.dp,
      modifier = Modifier.size(48.dp),
    )
    Spacer(Modifier.width(16.dp))
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = book.entry.title,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = supportingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

/** "1", "1.5" — reading-order position without a trailing ".0". */
internal fun formatPosition(position: Double): String {
  return if (position % 1.0 == 0.0) position.toInt().toString() else position.toString()
}
