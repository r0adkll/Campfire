package app.campfire.ui.settings.panes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Dangerous
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.audioplayer.offline.OfflineDownload.State.Completed
import app.campfire.audioplayer.offline.OfflineDownload.State.Downloading
import app.campfire.audioplayer.offline.OfflineDownload.State.Failed
import app.campfire.audioplayer.offline.OfflineDownload.State.None
import app.campfire.audioplayer.offline.OfflineDownload.State.Queued
import app.campfire.audioplayer.offline.OfflineDownload.State.Stopped
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Download
import app.campfire.common.compose.widgets.CoverImage
import app.campfire.common.compose.widgets.EmptyState
import app.campfire.core.extensions.asReadableBytes
import app.campfire.core.extensions.ifNotEmpty
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.LibraryItemId
import app.campfire.ui.settings.SettingsUiEvent
import app.campfire.ui.settings.SettingsUiState
import app.campfire.ui.settings.composables.ActionSetting
import app.campfire.ui.settings.composables.ConfirmationLayout
import app.campfire.ui.settings.composables.Header
import app.campfire.ui.settings.composables.SwitchSetting
import campfire.features.settings.ui.generated.resources.Res
import campfire.features.settings.ui.generated.resources.action_delete_download
import campfire.features.settings.ui.generated.resources.action_stop_download
import campfire.features.settings.ui.generated.resources.download_header_downloads
import campfire.features.settings.ui.generated.resources.label_confirm_download_delete
import campfire.features.settings.ui.generated.resources.label_confirm_download_stop
import campfire.features.settings.ui.generated.resources.setting_downloads_title
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_description
import campfire.features.settings.ui.generated.resources.setting_show_download_confirmation_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DownloadsPane(
  state: SettingsUiState,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingPaneLayout(
    title = { Text(stringResource(Res.string.setting_downloads_title)) },
    onBackClick = onBackClick,
    modifier = modifier,
  ) {
    SwitchSetting(
      value = state.downloadsSettings.showDownloadConfirmation,
      onValueChange = { state.eventSink(SettingsUiEvent.DownloadsSettingEvent.ShowDownloadConfirmation(it)) },
      headlineContent = { Text(stringResource(Res.string.setting_show_download_confirmation_title)) },
      supportingContent = { Text(stringResource(Res.string.setting_show_download_confirmation_description)) },
    )

    Header(title = { Text(stringResource(Res.string.download_header_downloads)) })

    state.downloadsSettings.downloads.ifNotEmpty {
      var showConfirmation by remember { mutableStateOf<LibraryItemId?>(null) }
      forEach { (item, download) ->
        ConfirmationLayout(
          showConfirmation = showConfirmation == item.id,
          confirm = {
            ConfirmDeleteListItem(
              download = download,
              onDeleteClick = {
                state.eventSink(
                  SettingsUiEvent.DownloadsSettingEvent.DeleteDownload(item),
                )
              },
              onDismissRequest = {
                showConfirmation = null
              },
            )
          },
        ) {
          ItemDownloadListItem(
            item = item,
            download = download,
            onClick = {
              state.eventSink(
                SettingsUiEvent.DownloadsSettingEvent.DownloadClicked(item),
              )
            },
            onDeleteClick = {
              showConfirmation = item.id
            },
          )
        }
      }
    }

    if (state.downloadsSettings.downloads.isEmpty()) {
      EmptyState(
        message = "No downloads yet!",
        modifier = Modifier
          .height(700.dp)
          .padding(vertical = 24.dp),
      )
    }
  }
}

@Composable
private fun ConfirmDeleteListItem(
  download: OfflineDownload,
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

        Completed,
        Failed,
        None,
        -> stringResource(Res.string.label_confirm_download_delete)
      },
      style = MaterialTheme.typography.bodyMedium,
      modifier = Modifier
        .weight(1f),
    )

    Spacer(Modifier.width(16.dp))

    OutlinedIconButton(
      onClick = onDismissRequest,
    ) {
      Icon(
        Icons.Rounded.Clear,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
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
          -> Icons.Rounded.Dangerous

          Stopped,
          Completed,
          Failed,
          None,
          -> Icons.Rounded.DeleteForever
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

          Completed,
          Failed,
          None,
          -> stringResource(Res.string.action_delete_download)
        },
      )
    }
  }
}

@Composable
private fun ItemDownloadListItem(
  item: LibraryItem,
  download: OfflineDownload,
  onClick: () -> Unit,
  onDeleteClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  ActionSetting(
    headlineContent = { Text(item.media.metadata.title ?: "<unknown item>") },
    supportingContent = { Text(item.media.sizeInBytes.asReadableBytes()) },
    leadingContent = {
      ItemDownloadImage(
        item = item,
        download = download,
      )
    },
    trailingContent = {
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
            -> Icons.Rounded.Dangerous

            Stopped,
            Completed,
            Failed,
            None,
            -> Icons.Rounded.Delete
          },
          contentDescription = null,
        )
      }
    },
    onClick = onClick,
    modifier = modifier,
  )
}

@Composable
private fun ItemDownloadImage(
  item: LibraryItem,
  download: OfflineDownload,
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
      shape = MaterialTheme.shapes.medium,
      size = size,
    )

    if (
      download.state != None &&
      download.state != Completed
    ) {
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
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          when (download.state) {
            Stopped,
            None,
            -> CampfireIcons.Rounded.Download

            Queued,
            Downloading,
            -> Icons.Rounded.Downloading

            Failed -> Icons.Rounded.WarningAmber
            Completed -> Icons.Rounded.DownloadDone
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
