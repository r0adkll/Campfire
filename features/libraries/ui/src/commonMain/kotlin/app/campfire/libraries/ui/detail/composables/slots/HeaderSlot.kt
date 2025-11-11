package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

class HeaderSlot(
  private val title: String,
) : ContentSlot {

  override val id: String = title
  override val contentType = ContentSlot.ContentType.Header

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    MetadataHeader(
      title = title,
      modifier = modifier.padding(
        horizontal = 16.dp,
      ),
    )
  }
}
