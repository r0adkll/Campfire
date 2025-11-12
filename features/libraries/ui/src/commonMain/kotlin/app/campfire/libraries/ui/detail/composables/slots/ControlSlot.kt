package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ControlBar
import app.campfire.libraries.ui.detail.dialog.ConfirmDownloadDialog
import app.campfire.libraries.ui.detail.permission.PermissionState
import app.campfire.libraries.ui.detail.permission.rememberPostNotificationPermissionState

class ControlSlot(
  private val libraryItem: LibraryItem,
  private val offlineDownload: OfflineDownload?,
  private val mediaProgress: MediaProgress?,
  @VisibleForTesting val showConfirmDownloadDialogSetting: Boolean,
) : ContentSlot {

  override val id: String = "control_bar"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    var showConfirmDownloadDialog by remember { mutableStateOf(false) }
    var doNotShowDownloadConfirmationAgain by remember { mutableStateOf(false) }
    val postNotificationPermissionState = rememberPostNotificationPermissionState {
      if (it) {
        eventSink(LibraryItemUiEvent.DownloadClick(doNotShowDownloadConfirmationAgain))
        showConfirmDownloadDialog = false
      }
    }

    ControlBar(
      offlineDownload = offlineDownload,
      mediaProgress = mediaProgress,
      onPlayClick = {
        eventSink(LibraryItemUiEvent.PlayClick(libraryItem))
      },
      onDownloadClick = {
        if (showConfirmDownloadDialogSetting) {
          showConfirmDownloadDialog = true
        } else {
          eventSink(LibraryItemUiEvent.DownloadClick())
        }
      },
      onMarkFinished = {
        eventSink(LibraryItemUiEvent.MarkFinished(libraryItem))
      },
      onMarkNotFinished = {
        eventSink(LibraryItemUiEvent.MarkNotFinished(libraryItem))
      },
      onDiscardProgress = {
        eventSink(LibraryItemUiEvent.DiscardProgress(libraryItem))
      },
      modifier = modifier.padding(horizontal = 16.dp),
    )

    if (showConfirmDownloadDialog) {
      ConfirmDownloadDialog(
        item = libraryItem,
        onConfirm = { doNotShowAgain ->
          if (postNotificationPermissionState is PermissionState.Granted) {
            eventSink(LibraryItemUiEvent.DownloadClick(doNotShowAgain))
            showConfirmDownloadDialog = false
          } else {
            doNotShowDownloadConfirmationAgain = doNotShowAgain
            postNotificationPermissionState.launchPermissionRequest()
          }
        },
        onDismissRequest = { showConfirmDownloadDialog = false },
      )
    }
  }
}
