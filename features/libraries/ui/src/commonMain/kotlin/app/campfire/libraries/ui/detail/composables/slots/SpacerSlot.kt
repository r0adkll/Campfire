package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

class SpacerSlot private constructor(
  override val id: String,
  private val height: Dp,
) : ContentSlot {

  override val contentType = ContentSlot.ContentType.Spacer

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    Spacer(
      modifier = modifier
        .height(height),
    )
  }

  companion object {
    fun small(id: String) = SpacerSlot(id, 8.dp)
    fun medium(id: String) = SpacerSlot(id, 16.dp)
    fun large(id: String) = SpacerSlot(id, 24.dp)
    fun xlarge(id: String) = SpacerSlot(id, 32.dp)
  }
}
