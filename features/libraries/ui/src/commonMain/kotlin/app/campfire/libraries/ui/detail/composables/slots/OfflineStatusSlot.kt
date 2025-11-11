package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.audioplayer.offline.OfflineDownload
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.OfflineStatusCard

class OfflineStatusSlot(
  private val offlineDownload: OfflineDownload,
) : ContentSlot {

  override val id: String = "offline_status"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    OfflineStatusCard(
      offlineDownload = offlineDownload,
      onDeleteClick = {
        eventSink(LibraryItemUiEvent.RemoveDownloadClick)
      },
      onStopClick = {
        eventSink(LibraryItemUiEvent.StopDownloadClick)
      },
      modifier = modifier
        .padding(
          horizontal = 16.dp,
        ),
    )
  }
}
