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
import app.campfire.common.compose.icons.rounded.Dangerous
import app.campfire.common.compose.icons.rounded.Delete
import app.campfire.common.compose.icons.rounded.DeleteForever
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.icons.rounded.DownloadDone
import app.campfire.common.compose.icons.rounded.Downloading
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.Podcasts
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.widgets.CampfireTopAppBar
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.common.compose.widgets.IconButtonTooltip
import app.campfire.common.compose.widgets.NavigationBackButton
import app.campfire.common.compose.widgets.placeholderBookPainter
import app.campfire.common.screens.DownloadsScreen
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
import campfire.features.downloads.ui.generated.resources.action_delete_download
import campfire.features.downloads.ui.generated.resources.action_dismiss
import campfire.features.downloads.ui.generated.resources.action_stop_download
import campfire.features.downloads.ui.generated.resources.downloads_empty
import campfire.features.downloads.ui.generated.resources.downloads_title
import campfire.features.downloads.ui.generated.resources.label_confirm_download_delete
import campfire.features.downloads.ui.generated.resources.label_confirm_download_stop
import com.slack.circuit.codegen.annotations.CircuitInject
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
    if (state.downloads.isEmpty()) {
      EmptyState(
        message = stringResource(Res.string.downloads_empty),
        modifier = Modifier
          .fillMaxSize()
          .padding(paddingValues),
      )
    } else {
      DownloadList(
        downloads = state.downloads,
        onClick = { state.eventSink(DownloadsUiEvent.DownloadClick(it)) },
        onDeleteClick = { state.eventSink(DownloadsUiEvent.DeleteDownload(it)) },
        contentPadding = paddingValues,
      )
    }
  }
}

@Composable
private fun DownloadList(
  downloads: List<DownloadEntry>,
  onClick: (DownloadEntry) -> Unit,
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
      text = when (download.state) {
        Queued,
        Stopped,
        Downloading,
        -> stringResource(Res.string.label_confirm_download_stop)

        Downloaded,
        Failed,
        -> stringResource(Res.string.label_confirm_download_delete)
      },
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
        when (download.state) {
          Queued,
          Downloading,
          -> CampfireIcons.Rounded.Dangerous

          Stopped,
          Downloaded,
          Failed,
          -> CampfireIcons.Rounded.DeleteForever
        },
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(
        when (download.state) {
          Queued,
          Stopped,
          Downloading,
          -> stringResource(Res.string.action_stop_download)

          Downloaded,
          Failed,
          -> stringResource(Res.string.action_delete_download)
        },
      )
    }
  }
}

private val DownloadEntry.confirmKey: String
  get() = when (this) {
    is DownloadEntry.Book -> libraryItem.id
    is DownloadEntry.Episode -> "${libraryItem.id}:${episode.id}"
    is DownloadEntry.Ebook -> "${libraryItem.id}:ebook"
  }

/** Whether [this] holds more than one kind of download, so each row says which it is. */
internal val List<DownloadEntry>.hasMixedMediaTypes: Boolean
  get() = distinctBy { it::class }.size > 1

private val DownloadEntry.mediaTypeIcon: ImageVector
  get() = when (this) {
    is DownloadEntry.Book -> CampfireIcons.Rounded.Headphones
    is DownloadEntry.Ebook -> CampfireIcons.Rounded.MenuBook
    is DownloadEntry.Episode -> CampfireIcons.Rounded.Podcasts
  }

/**
 * The size of the download, after what it is: an ebook's format always, as its details show it,
 * and an audiobook's file format when [showFormat], next to other kinds of downloads.
 */
internal fun DownloadEntry.supportingText(showFormat: Boolean): String = when (this) {
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
    append(libraryItem.media.metadata.title ?: "<unknown podcast>")
    append(" · ")
    append(episode.sizeInBytes.asReadableBytes())
  }
}

@Composable
internal fun ItemDownloadListItem(
  entry: DownloadEntry,
  showMediaType: Boolean,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val item = entry.libraryItem
  val download = entry.download
  val titleText = when (entry) {
    is DownloadEntry.Book,
    is DownloadEntry.Ebook,
    -> item.media.metadata.title ?: "<unknown item>"
    is DownloadEntry.Episode -> entry.episode.title
  }
  ListItem(
    headlineContent = { Text(titleText) },
    supportingContent = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        if (showMediaType) {
          Icon(
            entry.mediaTypeIcon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(Modifier.width(6.dp))
        }
        Text(entry.supportingText(showFormat = showMediaType))
      }
    },
    leadingContent = {
      ItemDownloadImage(
        item = item,
        download = download,
      )
    },
    trailingContent = {
      val deleteOrStopLabel = stringResource(
        when (download.state) {
          Queued,
          Stopped,
          Downloading,
          -> Res.string.action_stop_download

          Downloaded,
          Failed,
          -> Res.string.action_delete_download
        },
      )
      IconButtonTooltip(text = deleteOrStopLabel) {
        FilledTonalIconButton(
          onClick = onDeleteClick,
          colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.error,
          ),
        ) {
          Icon(
            when (download.state) {
              Queued,
              Downloading,
              -> CampfireIcons.Rounded.Dangerous

              Stopped,
              Downloaded,
              Failed,
              -> CampfireIcons.Rounded.Delete
            },
            contentDescription = deleteOrStopLabel,
          )
        }
      }
    },
    colors = ListItemDefaults.colors(
      containerColor = Color.Transparent,
    ),
    modifier = modifier.clickable(onClick = onClick),
  )
}

@Composable
private fun ItemDownloadImage(
  item: LibraryItem,
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
      imageUrl = item.media.coverImageUrl,
      contentDescription = item.media.metadata.title,
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
            color = when (download.state) {
              Stopped,
              Failed,
              -> MaterialTheme.colorScheme.errorContainer.copy(0.90f)
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
            Stopped,
            -> CampfireIcons.Rounded.Download

            Queued,
            Downloading,
            -> CampfireIcons.Rounded.Downloading

            Failed -> CampfireIcons.Rounded.Warning
            Downloaded -> CampfireIcons.Rounded.DownloadDone
          },
          contentDescription = null,
          tint = when (download.state) {
            Stopped,
            Failed,
            -> MaterialTheme.colorScheme.error
            else -> Color.White
          },
        )
      }
    }
  }
}
