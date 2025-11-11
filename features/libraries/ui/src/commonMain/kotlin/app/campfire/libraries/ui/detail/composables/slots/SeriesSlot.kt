package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.widgets.MetadataHeader
import app.campfire.core.model.LibraryItem
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.SeriesMetadata

class SeriesSlot(
  private val libraryItem: LibraryItem,
  private val seriesBooks: List<LibraryItem>,
) : ContentSlot {

  override val id: String = "series"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Column(modifier) {
      MetadataHeader(
        title = "Series",
        modifier = Modifier.padding(
          horizontal = 16.dp,
        ),
      )
      Spacer(Modifier.height(8.dp))
      SeriesMetadata(
        seriesName = libraryItem.media.metadata.seriesSequence?.name
          ?: libraryItem.media.metadata.seriesName
          ?: "--",
        seriesBooks = seriesBooks,
        modifier = Modifier
          .clickable(
            onClick = {
              eventSink(LibraryItemUiEvent.SeriesClick(libraryItem))
            },
          )
          .padding(horizontal = 16.dp),
      )
    }
  }
}
