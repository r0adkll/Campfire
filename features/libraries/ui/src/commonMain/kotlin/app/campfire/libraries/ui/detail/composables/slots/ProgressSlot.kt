package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.MediaProgressBar

class ProgressSlot(
  @VisibleForTesting
  val mediaProgress: MediaProgress,
) : ContentSlot {

  override val id: String = "media_progress"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    MediaProgressBar(
      progress = mediaProgress,
      modifier = modifier
        .padding(horizontal = 20.dp),
    )
  }
}
