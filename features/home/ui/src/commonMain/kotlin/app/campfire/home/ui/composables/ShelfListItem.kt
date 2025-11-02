package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.MediaProgress
import app.campfire.core.model.ShelfEntity
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.ui.UiShelf

@Composable
fun ShelfListItem(
  shelf: UiShelf<ShelfEntity>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
  progressStatus: (LibraryItemId) -> MediaProgress?,
  onItemClick: (Any) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    ShelfHeader(shelf)
    ShelfContent(
      shelf = shelf,
      offlineStatus = offlineStatus,
      progressStatus = progressStatus,
      onItemClick = onItemClick,
    )
  }
}
