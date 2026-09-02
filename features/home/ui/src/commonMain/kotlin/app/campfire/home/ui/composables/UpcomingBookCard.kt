// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.home.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.ElevatedContentCard
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.core.extensions.capitalized
import app.campfire.core.model.ShelfEntity
import campfire.features.home.ui.generated.resources.Res
import campfire.features.home.ui.generated.resources.cd_book_cover
import campfire.features.home.ui.generated.resources.upcoming_shelf_tba
import campfire.features.home.ui.generated.resources.upcoming_view_all
import campfire.features.home.ui.generated.resources.upcoming_view_all_count
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/**
 * An upcoming-release shelf entry: square cover, title, and release date,
 * matching the layout of the other home shelf cards. Cards without a provider
 * URL render inert.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun UpcomingBookCard(
  entry: ShelfEntity.UpcomingBookShelfEntry,
  onClick: (() -> Unit)?,
  modifier: Modifier = Modifier,
) {
  ElevatedContentCard(
    onClick = onClick,
    modifier = modifier,
  ) {
    CoverImage(
      imageUrl = entry.coverUrl,
      contentDescription = stringResource(Res.string.cd_book_cover, entry.title),
      placeholder = placeholderBookPainter(),
      shape = MaterialTheme.shapes.largeIncreased,
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f),
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
        text = entry.releaseDate.asShelfDateLabel()
          ?: stringResource(Res.string.upcoming_shelf_tba),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
  }
}

/**
 * The trailing card of the upcoming shelf: same silhouette as the book cards,
 * with a forward affordance opening the full Upcoming screen. [total] is the
 * complete cached count, including the undated announcements the shelf hides.
 */
@Composable
internal fun UpcomingViewAllCard(
  total: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ElevatedContentCard(
    onClick = onClick,
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .background(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = CircleShape,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          Icons.AutoMirrored.Rounded.ArrowForward,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
    Column(
      modifier = Modifier.padding(vertical = 16.dp),
    ) {
      Text(
        text = stringResource(Res.string.upcoming_view_all),
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      Text(
        text = stringResource(Res.string.upcoming_view_all_count, total),
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(horizontal = 16.dp),
      )
    }
  }
}

/** "Dec 6, 2026" from a provider's yyyy-MM-dd(ish) date; null when unparseable. */
private fun String?.asShelfDateLabel(): String? {
  val date = this?.take(10) ?: return null
  val parsed = try {
    LocalDate.parse(date)
  } catch (e: IllegalArgumentException) {
    return null
  }
  return "${parsed.month.name.capitalized().take(3)} ${parsed.day}, ${parsed.year}"
}
