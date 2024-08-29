package app.campfire.home.ui.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.home.api.model.Shelf

@Composable
fun ShelfListItem(
  shelf: Shelf<*>,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
  ) {
    ShelfHeader(shelf)
    ShelfContent(shelf)
  }
}
