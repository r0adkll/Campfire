// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LibraryItemMeta
import app.campfire.common.compose.widgets.LibraryItemMetaRow
import app.campfire.common.compose.widgets.ShowMoreLessButton
import app.campfire.common.compose.widgets.coverImageShape
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.core.extensions.asReadableBytes
import app.campfire.downloads.ui.DownloadEntry
import app.campfire.downloads.ui.ShowDownloads
import app.campfire.downloads.ui.key
import app.campfire.downloads.ui.sizeInBytes
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.action_play_download
import campfire.features.downloads.ui.generated.resources.downloads_section_episodes_size
import campfire.features.downloads.ui.generated.resources.downloads_unknown_podcast
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A podcast's downloaded episodes together under its name, so they aren't scattered among other
 * shows'. The heading opens the podcast; each episode opens, plays, and swipes away to remove it
 * on its own. Past [CollapsedEpisodes] episodes, the rest wait behind a button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ShowDownloadsCard(
  show: ShowDownloads,
  onShowClick: () -> Unit,
  onEpisodeClick: (DownloadEntry.Episode) -> Unit,
  onPlayClick: (DownloadEntry.Episode) -> Unit,
  onDelete: (DownloadEntry.Episode) -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColor = MaterialTheme.colorScheme.surfaceContainerLow
  ElevatedCard(
    shape = MaterialTheme.shapes.large,
    colors = CardDefaults.elevatedCardColors(
      containerColor = containerColor,
      contentColor = MaterialTheme.colorScheme.onSurface,
    ),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .clickable(onClick = onShowClick)
        .padding(12.dp),
    ) {
      CoverImage(
        imageUrl = show.libraryItem.media.coverImageUrl,
        contentDescription = null,
        placeholder = placeholderBookPainter(),
        size = ShowCoverSize,
        shape = coverImageShape(ShowCoverSize),
      )
      Spacer(Modifier.size(12.dp))
      Column(Modifier.weight(1f)) {
        Text(
          text = show.libraryItem.media.metadata.title ?: stringResource(Res.string.downloads_unknown_podcast),
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = pluralStringResource(
            Res.plurals.downloads_section_episodes_size,
            show.episodes.size,
            show.episodes.size,
            show.sizeInBytes.asReadableBytes(),
          ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    var expanded by rememberSaveable { mutableStateOf(false) }
    val shown = if (expanded) show.episodes else show.episodes.take(CollapsedEpisodes)
    shown.forEach { entry ->
      // Keyed, or the episode moving up into a swiped-away row's place would take over its
      // swipe, and be deleted along with it
      key(entry.key) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        EpisodeDownloadRow(
          entry = entry,
          onClick = { onEpisodeClick(entry) },
          onPlayClick = { onPlayClick(entry) },
          onDelete = { onDelete(entry) },
          // Opaque, so the delete behind it only shows as it's swiped
          containerColor = containerColor,
        )
      }
    }

    if (show.episodes.size > CollapsedEpisodes) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp),
      ) {
        ShowMoreLessButton(
          expanded = expanded,
          onExpandedChange = { expanded = it },
        )
      }
    }
  }
}

@Composable
private fun EpisodeDownloadRow(
  entry: DownloadEntry.Episode,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  onDelete: () -> Unit,
  containerColor: Color,
  modifier: Modifier = Modifier,
) {
  SwipeToDelete(
    deleteLabel = stringResource(entry.download.deleteLabel),
    onDelete = onDelete,
    modifier = modifier,
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .weight(1f)
        .background(containerColor)
        .clickable(onClick = onClick)
        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          text = entry.episode.title,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.size(4.dp))
        LibraryItemMetaRow(
          meta = listOf(
            LibraryItemMeta.Size(entry.sizeInBytes),
            LibraryItemMeta.Length(entry.episode.duration),
          ),
        )
      }

      Spacer(Modifier.size(8.dp))

      val playLabel = stringResource(Res.string.action_play_download)
      IconButtonTooltip(text = playLabel) {
        FilledTonalIconButton(
          onClick = onPlayClick,
          shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            pressedShape = CircleShape,
          ),
        ) {
          Icon(CampfireIcons.Rounded.PlayArrow, contentDescription = playLabel)
        }
      }
    }
  }
}

private val ShowCoverSize = 48.dp

/** How many of a show's episodes are listed before the rest are asked for. */
internal const val CollapsedEpisodes = 3
