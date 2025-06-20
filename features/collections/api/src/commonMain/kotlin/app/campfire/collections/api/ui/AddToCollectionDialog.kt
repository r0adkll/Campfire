package app.campfire.collections.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItem

/**
 * Composable interface for providing the Add to Collection dialog to
 * other surfaces while obscuring its implementation
 */
interface AddToCollectionDialog {

  @Composable
  fun Content(
    item: LibraryItem,
    onDismiss: () -> Unit,
    modifier: Modifier,
  )
}
