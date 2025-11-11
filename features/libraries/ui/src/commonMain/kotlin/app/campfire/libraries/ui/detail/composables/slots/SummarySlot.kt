package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.ItemDescription

class SummarySlot(
  private val description: String,
) : ContentSlot {

  override val id: String = "summary"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(
      modifier = modifier,
    ) {
      MetadataHeader(
        title = "Summary",
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      Spacer(Modifier.height(8.dp))
      ItemDescription(
        description = description,
      )
    }
  }
}
