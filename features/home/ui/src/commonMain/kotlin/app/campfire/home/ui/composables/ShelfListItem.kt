package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItemId
import app.campfire.core.offline.OfflineStatus
import app.campfire.home.api.model.Shelf

@Composable
fun ShelfListItem(
  shelf: Shelf<*>,
  offlineStatus: (LibraryItemId) -> OfflineStatus,
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
      onItemClick = onItemClick,
    )
  }
}
