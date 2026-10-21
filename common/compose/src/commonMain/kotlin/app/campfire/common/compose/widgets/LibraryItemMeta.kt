// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.extensions.ReadoutStyle
import app.campfire.common.compose.extensions.asRelativeDayLabel
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.CalendarMonth
import app.campfire.common.compose.icons.rounded.EventAvailable
import app.campfire.common.compose.icons.rounded.LibraryAdd
import app.campfire.common.compose.icons.rounded.LibraryBooks
import app.campfire.common.compose.icons.rounded.Podcasts
import app.campfire.common.compose.icons.rounded.QueueMusic
import app.campfire.common.compose.icons.rounded.Schedule
import app.campfire.common.compose.icons.rounded.SdStorage
import app.campfire.common.compose.icons.rounded.Timer
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.duration_remaining
import campfire.common.compose.generated.resources.item_meta_episode_count
import campfire.common.compose.generated.resources.item_meta_series_position
import campfire.common.compose.generated.resources.item_meta_track_count
import kotlin.time.Duration
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A single fact about a library item, shown as an icon and a short label on list rows and cards.
 */
@Immutable
sealed interface LibraryItemMeta {
  data class Length(val duration: Duration) : LibraryItemMeta
  data class Remaining(val duration: Duration) : LibraryItemMeta
  data class Size(val bytes: Long) : LibraryItemMeta
  data class Episodes(val count: Int) : LibraryItemMeta
  data class Tracks(val count: Int) : LibraryItemMeta
  data class PublishedYear(val year: String) : LibraryItemMeta
  data class AddedAt(val epochMillis: Long) : LibraryItemMeta
  data class FinishedAt(val epochMillis: Long) : LibraryItemMeta
  data class SeriesPosition(val sequence: String) : LibraryItemMeta
}

/**
 * What an item shows when nothing more specific applies: an audiobook's length, an ebook's file
 * size (it has no length), or how many episodes a podcast has.
 */
val LibraryItem.defaultMeta: LibraryItemMeta
  get() = when {
    media is Media.Podcast -> LibraryItemMeta.Episodes((media as Media.Podcast).numEpisodes)
    isEbookOnly -> LibraryItemMeta.Size(media.sizeInBytes)
    else -> LibraryItemMeta.Length(media.duration)
  }

/**
 * Draws [meta] on one line. A line too long for its space scrolls instead of squeezing the later
 * facts.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryItemMetaRow(
  meta: List<LibraryItemMeta>,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.basicMarquee(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    meta.forEach { item ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          item.icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Text(
          text = item.label(),
          style = MaterialTheme.typography.labelSmallEmphasized,
          maxLines = 1,
        )
      }
    }
  }
}

private val LibraryItemMeta.icon: ImageVector
  get() = when (this) {
    is LibraryItemMeta.Length -> CampfireIcons.Rounded.Schedule
    is LibraryItemMeta.Remaining -> CampfireIcons.Rounded.Timer
    is LibraryItemMeta.Size -> CampfireIcons.Rounded.SdStorage
    is LibraryItemMeta.Episodes -> CampfireIcons.Rounded.Podcasts
    is LibraryItemMeta.Tracks -> CampfireIcons.Rounded.QueueMusic
    is LibraryItemMeta.PublishedYear -> CampfireIcons.Rounded.CalendarMonth
    is LibraryItemMeta.AddedAt -> CampfireIcons.Rounded.LibraryAdd
    is LibraryItemMeta.FinishedAt -> CampfireIcons.Rounded.EventAvailable
    is LibraryItemMeta.SeriesPosition -> CampfireIcons.Rounded.LibraryBooks
  }

@Composable
private fun LibraryItemMeta.label(): String = when (this) {
  is LibraryItemMeta.Length -> duration.thresholdReadoutFormat()
  is LibraryItemMeta.Remaining -> stringResource(Res.string.duration_remaining, duration.thresholdReadoutFormat())
  is LibraryItemMeta.Size -> bytes.asReadableBytes()
  is LibraryItemMeta.Episodes -> pluralStringResource(Res.plurals.item_meta_episode_count, count, count)
  is LibraryItemMeta.Tracks -> pluralStringResource(Res.plurals.item_meta_track_count, count, count)
  is LibraryItemMeta.PublishedYear -> year
  is LibraryItemMeta.AddedAt -> epochMillis.asDate().asRelativeDayLabel(ReadoutStyle.Short)
  is LibraryItemMeta.FinishedAt -> epochMillis.asDate().asRelativeDayLabel(ReadoutStyle.Short)
  is LibraryItemMeta.SeriesPosition -> stringResource(Res.string.item_meta_series_position, sequence)
}
