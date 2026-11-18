// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.downloads.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.CampfireWindowInsets
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Close
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.DownloadDone
import app.campfire.common.compose.icons.rounded.Downloading
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.Pause
import app.campfire.common.compose.icons.rounded.Podcasts
import app.campfire.common.compose.icons.rounded.Refresh
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.ErrorListState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.LoadingListState
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.screens.DownloadsScreen
import app.campfire.core.coroutines.LoadState
import app.campfire.core.di.UserScope
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.model.LibraryItem
import app.campfire.offline.Download
import app.campfire.offline.Download.State.Downloaded
import app.campfire.offline.Download.State.Downloading
import app.campfire.offline.Download.State.Failed
import app.campfire.offline.Download.State.Queued
import app.campfire.offline.Download.State.Stopped
import campfire.features.downloads.ui.generated.resources.Res
import campfire.features.downloads.ui.generated.resources.action_cancel_download
import campfire.features.downloads.ui.generated.resources.action_delete_download
import campfire.features.downloads.ui.generated.resources.action_dismiss
import campfire.features.downloads.ui.generated.resources.action_resume_download
import campfire.features.downloads.ui.generated.resources.action_retry_download
import campfire.features.downloads.ui.generated.resources.downloads_empty
import campfire.features.downloads.ui.generated.resources.downloads_error
import campfire.features.downloads.ui.generated.resources.downloads_title
import campfire.features.downloads.ui.generated.resources.downloads_unavailable_description
import campfire.features.downloads.ui.generated.resources.downloads_unavailable_title
import campfire.features.downloads.ui.generated.resources.downloads_unknown_item
import campfire.features.downloads.ui.generated.resources.downloads_unknown_podcast
import campfire.features.downloads.ui.generated.resources.label_confirm_download_cancel
import campfire.features.downloads.ui.generated.resources.label_confirm_download_delete
import com.slack.circuit.codegen.annotations.CircuitInject
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@CircuitInject(DownloadsScreen::class, UserScope::class)
@Composable
fun DownloadsUi(
  state: DownloadsUiState,
  modifier: Modifier = Modifier,
) {
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    topBar = {
      CampfireTopAppBar(
        title = { Text(stringResource(Res.string.downloads_title)) },
        navigationIcon = {
          NavigationBackButton(onClick = { state.eventSink(DownloadsUiEvent.Back) })
        },
        scrollBehavior = scrollBehavior,
      )
    },
    contentWindowInsets = CampfireWindowInsets,
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
  ) { paddingValues ->
    when (val downloads = state.downloads) {
      LoadState.Loading -> LoadingListState(Modifier.padding(paddingValues))

      LoadState.Error -> ErrorListState(
        message = stringResource(Res.string.downloads_error),
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      )

      is LoadState.Loaded -> if (downloads.data.isEmpty()) {
        EmptyState(
          message = stringResource(Res.string.downloads_empty),
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      } else {
        DownloadList(
          downloads = downloads.data,
          onClick = { state.eventSink(DownloadsUiEvent.DownloadClick(it)) },
          onRetryClick = { state.eventSink(DownloadsUiEvent.RetryDownload(it)) },
          onDeleteClick = { state.eventSink(DownloadsUiEvent.DeleteDownload(it)) },
          contentPadding = paddingValues,
        )
      }
    }
  }
}

@Composable
private fun DownloadList(
  downloads: List<DownloadEntry>,
  onClick: (DownloadEntry) -> Unit,
  onRetryClick: (DownloadEntry) -> Unit,
  onDeleteClick: (DownloadEntry) -> Unit,
  modifier: Modifier = Modifier,
  contentPadding: PaddingValues = PaddingValues(),
) {
  var confirmingKey by remember { mutableStateOf<String?>(null) }
  val showMediaType = downloads.hasMixedMediaTypes
  LazyColumn(
    contentPadding = contentPadding,
    modifier = modifier.fillMaxSize(),
  ) {
    items(
      items = downloads,
      key = { it.confirmKey },
    ) { entry ->
      val key = entry.confirmKey
      ConfirmationLayout(
        showConfirmation = confirmingKey == key,
        confirm = {
          ConfirmDeleteListItem(
            download = entry.download,
            onDeleteClick = { onDeleteClick(entry) },
            onDismissRequest = { confirmingKey = null },
          )
        },
      ) {
        ItemDownloadListItem(
          entry = entry,
          showMediaType = showMediaType,
          onClick = { onClick(entry) },
          onRetryClick = { onRetryClick(entry) },
          onDeleteClick = { confirmingKey = key },
        )
      }
    }
  }
}

@Composable
private fun ConfirmDeleteListItem(
  download: Download,
  onDeleteClick: () -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .background(MaterialTheme.colorScheme.secondaryContainer)
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = stringResource(
        if (download.isInProgress) {
          Res.string.label_confirm_download_cancel
        } else {
          Res.string.label_confirm_download_delete
        },
      ),
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier
        .weight(1f),
    )

    Spacer(Modifier.width(16.dp))

    val dismissLabel = stringResource(Res.string.action_dismiss)
    IconButtonTooltip(text = dismissLabel) {
      OutlinedIconButton(
        onClick = onDismissRequest,
      ) {
        Icon(
          CampfireIcons.Rounded.Close,
          contentDescription = dismissLabel,
          modifier = Modifier.size(ButtonDefaults.IconSize),
        )
      }
    }

    Spacer(Modifier.width(4.dp))

    Button(
      onClick = onDeleteClick,
      contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
    ) {
      Icon(
        CampfireIcons.Rounded.DeleteForever,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(download.deleteLabel))
    }
  }
}

private val DownloadEntry.confirmKey: String
  get() = when (this) {
    is DownloadEntry.Book -> libraryItem.id
    is DownloadEntry.Episode -> "${libraryItem.id}:${episode.id}"
    is DownloadEntry.Ebook -> "${libraryItem.id}:ebook"
    is DownloadEntry.Unavailable -> "${download.content.itemId}:unavailable"
  }

/**
 * What removing the download is called. Removing one that's still going cancels it and keeps
 * nothing of it, so it's never called stopping or pausing.
 */
private val Download.deleteLabel: StringResource
  get() = if (isInProgress) Res.string.action_cancel_download else Res.string.action_delete_download

/**
 * Whether [this] holds more than one kind of download, so each row says which it is. Downloads
 * whose item can't be found aren't a kind of their own.
 */
internal val List<DownloadEntry>.hasMixedMediaTypes: Boolean
  get() = filterNot { it is DownloadEntry.Unavailable }.distinctBy { it::class }.size > 1

private val DownloadEntry.mediaTypeIcon: ImageVector
  get() = when (this) {
    is DownloadEntry.Book -> CampfireIcons.Rounded.Headphones
    is DownloadEntry.Ebook -> CampfireIcons.Rounded.MenuBook
    is DownloadEntry.Episode -> CampfireIcons.Rounded.Podcasts
    is DownloadEntry.Unavailable -> CampfireIcons.Rounded.Warning
  }

/**
 * The size of the download, after what it is: an ebook's format always, as its details show it,
 * and an audiobook's file format when [showFormat], next to other kinds of downloads. An episode
 * of a podcast without a title shows [unknownPodcast], and a download whose item can't be found
 * shows only what it takes up, or nothing when that isn't known.
 */
internal fun DownloadEntry.supportingText(showFormat: Boolean, unknownPodcast: String): String = when (this) {
  is DownloadEntry.Book -> {
    val size = libraryItem.media.sizeInBytes.asReadableBytes()
    // Only a single format reads cleanly; a book mixing them just shows its size
    val format = libraryItem.media.tracks
      .map { it.metadata.ext.removePrefix(".").uppercase() }
      .filter { it.isNotBlank() }
      .distinct()
      .singleOrNull()
    if (showFormat && format != null) "$format · $size" else size
  }
  is DownloadEntry.Ebook -> {
    val size = download.totalBytes
      .takeIf { it > 0L }
      ?.asReadableBytes()
      ?: libraryItem.media.sizeInBytes.asReadableBytes()
    val format = libraryItem.media.ebookFormat?.uppercase()
    if (format != null) "$format · $size" else size
  }
  is DownloadEntry.Episode -> buildString {
    append(libraryItem.media.metadata.title ?: unknownPodcast)
    append(" · ")
    append(episode.sizeInBytes.asReadableBytes())
  }
  is DownloadEntry.Unavailable -> if (sizeInBytes > 0L) sizeInBytes.asReadableBytes() else ""
}

@Composable
internal fun ItemDownloadListItem(
  entry: DownloadEntry,
  showMediaType: Boolean,
  onClick: () -> Unit,
  onRetryClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val item = entry.libraryItem
  val download = entry.download
  val titleText = when (entry) {
    is DownloadEntry.Book -> entry.libraryItem.media.metadata.title
    is DownloadEntry.Ebook -> entry.libraryItem.media.metadata.title
    is DownloadEntry.Episode -> entry.episode.title
    is DownloadEntry.Unavailable -> stringResource(Res.string.downloads_unavailable_title)
  } ?: stringResource(Res.string.downloads_unknown_item)
  val details = entry.supportingText(
    showFormat = showMediaType,
    unknownPodcast = stringResource(Res.string.downloads_unknown_podcast),
  )
  val supportingText = if (entry is DownloadEntry.Unavailable) {
    listOf(stringResource(Res.string.downloads_unavailable_description), details)
      .filter { it.isNotEmpty() }
      .joinToString(" · ")
  } else {
    details
  }
  ListItem(
    headlineContent = { Text(titleText) },
    supportingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (showMediaType || entry is DownloadEntry.Unavailable) {
          Icon(
            entry.mediaTypeIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
        }
        Text(supportingText)
      }
    },
    leadingContent = {
      ItemDownloadImage(
        item = item,
        download = download,
      )
    },
    trailingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // A failed or stopped download starts again from here, rather than from its item
        if (entry !is DownloadEntry.Unavailable && (download.state == Failed || download.state == Stopped)) {
          val retryLabel = stringResource(
            if (download.state == Failed) Res.string.action_retry_download else Res.string.action_resume_download,
          )
          IconButtonTooltip(text = retryLabel) {
            FilledTonalIconButton(onClick = onRetryClick) {
              Icon(CampfireIcons.Rounded.Refresh, contentDescription = retryLabel)
            }
          }
          Spacer(Modifier.width(4.dp))
        }

        val deleteLabel = stringResource(download.deleteLabel)
        IconButtonTooltip(text = deleteLabel) {
          FilledTonalIconButton(
            onClick = onDeleteClick,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
              containerColor = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.error,
            ),
          ) {
            Icon(
              if (download.isInProgress) CampfireIcons.Rounded.Close else CampfireIcons.Rounded.Delete,
              contentDescription = deleteLabel,
            )
          }
        }
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    // There's nothing to open for an item that can't be found
    modifier = modifier.clickable(enabled = item != null, onClick = onClick),
  )
}

@Composable
private fun ItemDownloadImage(
  item: LibraryItem?,
  download: Download,
  modifier: Modifier = Modifier,
  size: Dp = 56.dp,
) {
  Box(
    modifier = modifier
      .clip(MaterialTheme.shapes.medium),
    contentAlignment = Alignment.Center,
  ) {
    CoverImage(
      imageUrl = item?.media?.coverImageUrl,
      contentDescription = item?.media?.metadata?.title,
      placeholder = placeholderBookPainter(),
      shape = MaterialTheme.shapes.medium,
      size = size,
    )

    if (
      download.state != Downloaded
    ) {
      val downloadingColor = MaterialTheme.colorScheme.primaryContainer
        .copy(alpha = 0.75f)
      Box(
        modifier = Modifier
          .size(size)
          .background(
            // Only a failure reads as an error: a stopped download just waits to be resumed
            color = when (download.state) {
              Failed -> MaterialTheme.colorScheme.errorContainer.copy(0.90f)
              else -> MaterialTheme.colorScheme.scrim.copy(0.6f)
            },
          )
          .drawWithContent {
            if (download.state == Downloading) {
              val padding = 16.dp.toPx()
              drawArc(
                color = downloadingColor,
                startAngle = -90f,
                sweepAngle = 360f * (download.progress ?: 0f),
                topLeft = Offset(-padding, -padding),
                size = Size(this.size.width + (padding * 2), this.size.height + (padding * 2)),
                useCenter = true,
              )
            }

            drawContent()
          },
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          when (download.state) {
            Stopped -> CampfireIcons.Rounded.Pause

            Queued,
            Downloading,
            -> CampfireIcons.Rounded.Downloading

            Failed -> CampfireIcons.Rounded.Warning
            Downloaded -> CampfireIcons.Rounded.DownloadDone
          },
          contentDescription = null,
          tint = when (download.state) {
            Failed -> MaterialTheme.colorScheme.error
            else -> Color.White
          },
        )
      }
    }
  }
}
