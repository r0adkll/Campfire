// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.Downloading
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.PlayArrow
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LibraryItemListItem
import app.campfire.common.compose.widgets.LibraryItemMeta
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.compose.widgets.swipetodismiss.AnimatedRemoveBackgroundContent
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBox
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxDefaults
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxState
import app.campfire.common.compose.widgets.swipetodismiss.SwipeToDismissBoxValue
import app.campfire.core.extensions.asReadableBytes
import app.campfire.downloads.ui.DownloadEntry
import app.campfire.downloads.ui.formatLabel
import app.campfire.downloads.ui.key
import app.campfire.downloads.ui.percent
import app.campfire.downloads.ui.sizeInBytes
import app.campfire.offline.Download
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.action_cancel_download
import campfire.features.downloads.ui.generated.resources.action_delete_download
import campfire.features.downloads.ui.generated.resources.action_play_download
import campfire.features.downloads.ui.generated.resources.action_read_download
import campfire.features.downloads.ui.generated.resources.action_resume_download
import campfire.features.downloads.ui.generated.resources.action_retry_download
import campfire.features.downloads.ui.generated.resources.download_status_failed
import campfire.features.downloads.ui.generated.resources.download_status_progress
import campfire.features.downloads.ui.generated.resources.download_status_progress_bytes
import campfire.features.downloads.ui.generated.resources.download_status_queued
import campfire.features.downloads.ui.generated.resources.download_status_stopped
import campfire.features.downloads.ui.generated.resources.downloads_unavailable_description
import campfire.features.downloads.ui.generated.resources.downloads_unavailable_title
import campfire.features.downloads.ui.generated.resources.downloads_unknown_item
import campfire.features.downloads.ui.generated.resources.downloads_unknown_podcast
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * A download's row, swiped toward the start of the line to take it off the list, as playlist and
 * queue rows are. A finished download shows as its item does in other lists, with a button to play
 * or read it; one still going, stopped, failed, or without its item says where it stands instead.
 *
 * @param onDelete called to take the download off the list, by a swipe, the button on a row that
 *   has one, or the accessibility action every row offers.
 */
@Composable
internal fun DownloadRow(
  entry: DownloadEntry,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  onRetryClick: () -> Unit,
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Remembered rather than saved, so a row put back by Undo isn't restored swiped away
  val positionalThreshold = SwipeToDismissBoxDefaults.positionalThreshold
  val swipeState = remember { SwipeToDismissBoxState(SwipeToDismissBoxValue.Settled, positionalThreshold) }
  val deleteLabel = stringResource(entry.download.deleteLabel)
  SwipeToDismissBox(
    state = swipeState,
    enableDismissFromStartToEnd = false,
    onDismiss = {
      if (it != SwipeToDismissBoxValue.Settled) onDelete()
    },
    backgroundContent = {
      Spacer(Modifier.weight(1f))
      AnimatedRemoveBackgroundContent(swipeState)
    },
    modifier = modifier.semantics {
      customActions = listOf(
        CustomAccessibilityAction(deleteLabel) {
          onDelete()
          true
        },
      )
    },
  ) {
    if (entry.download.state == Download.State.Downloaded && entry !is DownloadEntry.Unavailable) {
      DownloadedRow(
        entry = entry,
        onClick = onClick,
        onPlayClick = onPlayClick,
        modifier = Modifier.weight(1f),
      )
    } else {
      DownloadStatusRow(
        entry = entry,
        onClick = onClick,
        onRetryClick = onRetryClick,
        onDeleteClick = onDelete,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

/** What the entry is called: its book's title, or its episode's. */
@Composable
internal fun DownloadEntry.title(): String = when (this) {
  is DownloadEntry.Book -> libraryItem.media.metadata.title
  is DownloadEntry.Ebook -> libraryItem.media.metadata.title
  is DownloadEntry.Episode -> episode.title
  is DownloadEntry.Unavailable -> stringResource(Res.string.downloads_unavailable_title)
} ?: stringResource(Res.string.downloads_unknown_item)

/** Who it's by: a book's author, or the podcast an episode belongs to. */
@Composable
private fun DownloadEntry.subtitle(): String = when (this) {
  is DownloadEntry.Book -> libraryItem.media.metadata.authorName
  is DownloadEntry.Ebook -> libraryItem.media.metadata.authorName
  is DownloadEntry.Episode -> libraryItem.media.metadata.title ?: stringResource(Res.string.downloads_unknown_podcast)
  is DownloadEntry.Unavailable -> stringResource(Res.string.downloads_unavailable_description)
} ?: "--"

/**
 * What removing the download is called. Removing one that's still going cancels it and keeps
 * nothing of it, so it's never called stopping or pausing.
 */
private val Download.deleteLabel: StringResource
  get() = if (isInProgress) Res.string.action_cancel_download else Res.string.action_delete_download

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadedRow(
  entry: DownloadEntry,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val item = entry.libraryItem ?: return
  val episode = (entry as? DownloadEntry.Episode)?.episode
  LibraryItemListItem(
    libraryItem = item,
    onClick = onClick,
    // An item's audio and its ebook are rows of their own, which the item's id can't tell apart
    sharedTransitionKey = entry.key,
    titleOverride = episode?.title,
    subtitleOverride = episode?.let { entry.subtitle() },
    // The format sits with the other facts for every kind, so an ebook's isn't also on its cover
    showEbookFormat = false,
    meta = buildList {
      entry.formatLabel?.let { add(LibraryItemMeta.Format(it)) }
      add(LibraryItemMeta.Size(entry.sizeInBytes))
      when (entry) {
        is DownloadEntry.Book -> add(LibraryItemMeta.Length(item.media.duration))
        is DownloadEntry.Episode -> add(LibraryItemMeta.Length(entry.episode.duration))
        else -> Unit
      }
    },
    // The actions are the item's, so a row standing for one of its episodes doesn't offer them
    offersActions = entry !is DownloadEntry.Episode,
    trailingContent = {
      val reads = entry is DownloadEntry.Ebook
      val playLabel = stringResource(if (reads) Res.string.action_read_download else Res.string.action_play_download)
      IconButtonTooltip(text = playLabel) {
        FilledTonalIconButton(
          onClick = onPlayClick,
          shapes = IconButtonDefaults.shapes(
            shape = MaterialTheme.shapes.small,
            pressedShape = CircleShape,
          ),
        ) {
          Icon(
            if (reads) CampfireIcons.Rounded.MenuBook else CampfireIcons.Rounded.PlayArrow,
            contentDescription = playLabel,
          )
        }
      }
    },
    modifier = modifier,
  )
}

/**
 * The row of a download that can't be played yet: still going, stopped, failed, or kept for an
 * item that can't be found. It's shaped like [LibraryItemListItem], with the download's standing
 * where the item's details would be.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DownloadStatusRow(
  entry: DownloadEntry,
  onClick: () -> Unit,
  onRetryClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val download = entry.download
  val unavailable = entry is DownloadEntry.Unavailable
  val shape = MaterialTheme.shapes.large
  ElevatedCard(
    onClick = onClick,
    // There's nothing to open for an item that can't be found
    enabled = !unavailable,
    shape = shape,
    colors = CardDefaults.elevatedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      contentColor = MaterialTheme.colorScheme.onSurface,
      disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      disabledContentColor = MaterialTheme.colorScheme.onSurface,
    ),
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      DownloadStatusCover(entry = entry)

      Spacer(Modifier.size(16.dp))

      Column(Modifier.weight(1f)) {
        Text(
          text = entry.title(),
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = entry.subtitle(),
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.size(6.dp))

        if (!unavailable) {
          when (download.state) {
            Download.State.Queued -> LinearProgressIndicator(Modifier.fillMaxWidth())

            Download.State.Downloading,
            Download.State.Stopped,
            -> LinearProgressIndicator(
              progress = { download.progress ?: 0f },
              // A stopped download isn't going anywhere, so its bar doesn't look as though it is
              color = if (download.state == Download.State.Stopped) {
                MaterialTheme.colorScheme.outline
              } else {
                MaterialTheme.colorScheme.primary
              },
              modifier = Modifier.fillMaxWidth(),
            )

            Download.State.Failed,
            Download.State.Downloaded,
            -> Unit
          }
          Spacer(Modifier.size(4.dp))
        }

        Text(
          text = entry.statusText(),
          style = MaterialTheme.typography.labelMedium,
          // Only a failure reads as an error: a stopped download just waits to be resumed
          color = if (!unavailable && download.state == Download.State.Failed) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // A failed or stopped download starts again from here, rather than from its item
      if (!unavailable && (download.state == Download.State.Failed || download.state == Download.State.Stopped)) {
        val retryLabel = stringResource(
          if (download.state == Download.State.Failed) {
            Res.string.action_retry_download
          } else {
            Res.string.action_resume_download
          },
        )
        IconButtonTooltip(text = retryLabel) {
          FilledTonalIconButton(
            onClick = onRetryClick,
            shapes = IconButtonDefaults.shapes(
              shape = MaterialTheme.shapes.small,
              pressedShape = CircleShape,
            ),
          ) {
            Icon(CampfireIcons.Rounded.Refresh, contentDescription = retryLabel)
          }
        }
      }

      // These rows need doing something about, so removing them doesn't take knowing the swipe
      val deleteLabel = stringResource(download.deleteLabel)
      IconButtonTooltip(text = deleteLabel) {
        IconButton(onClick = onDeleteClick) {
          Icon(
            if (download.isInProgress) CampfireIcons.Rounded.Close else CampfireIcons.Rounded.Delete,
            contentDescription = deleteLabel,
          )
        }
      }

      Spacer(Modifier.size(4.dp))
    }
  }
}

/** Where the download stands, in a line: how far along it is, or why it isn't going. */
@Composable
private fun DownloadEntry.statusText(): String {
  if (this is DownloadEntry.Unavailable) {
    return if (sizeInBytes > 0L) sizeInBytes.asReadableBytes() else ""
  }
  return when (download.state) {
    Download.State.Queued -> stringResource(Res.string.download_status_queued)
    Download.State.Stopped -> stringResource(Res.string.download_status_stopped)
    Download.State.Failed -> stringResource(Res.string.download_status_failed)
    Download.State.Downloading,
    Download.State.Downloaded,
    -> {
      val percent = percent ?: 0
      if (download.totalBytes > 0L) {
        stringResource(
          Res.string.download_status_progress_bytes,
          percent,
          download.bytes.asReadableBytes(),
          download.totalBytes.asReadableBytes(),
        )
      } else {
        stringResource(Res.string.download_status_progress, percent)
      }
    }
  }
}

@Composable
private fun DownloadStatusCover(
  entry: DownloadEntry,
  modifier: Modifier = Modifier,
) {
  val shape = MaterialTheme.shapes.large
  Box(
    modifier = modifier.clip(shape),
    contentAlignment = Alignment.Center,
  ) {
    CoverImage(
      imageUrl = entry.libraryItem?.media?.coverImageUrl,
      contentDescription = null,
      placeholder = placeholderBookPainter(),
      shape = shape,
      size = ThumbnailSize,
    )

    if (entry !is DownloadEntry.Unavailable) {
      val failed = entry.download.state == Download.State.Failed
      Box(
        modifier = Modifier
          .size(ThumbnailSize)
          .background(
            if (failed) {
              MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
            } else {
              MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
            },
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          when (entry.download.state) {
            Download.State.Stopped -> CampfireIcons.Rounded.Pause
            Download.State.Failed -> CampfireIcons.Rounded.Warning
            else -> CampfireIcons.Rounded.Downloading
          },
          contentDescription = null,
          tint = if (failed) MaterialTheme.colorScheme.error else Color.White,
        )
      }
    }
  }
}

// As LibraryItemListItem's, so the rows line up
private val ThumbnailSize = 88.dp
