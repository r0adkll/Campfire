package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

interface ContentSlot {

  /**
   * Unique slot identifier for efficient [androidx.compose.foundation.lazy.LazyColumn] items
   */
  val id: String

  /**
   * Content type of the slot. This is used for optimizations in [androidx.compose.foundation.lazy.LazyColumn]
   */
  val contentType: Any? get() = null

  @Composable
  fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit)

  enum class ContentType {
    Header,
    Chips,
    Chapter,
    Spacer,
  }
}
